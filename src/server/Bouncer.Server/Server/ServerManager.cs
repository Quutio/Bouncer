using Bouncer.Grpc;
using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics.CodeAnalysis;
using System.Threading;
using System.Threading.Tasks;
using Bouncer.Server.Internal;
using Bouncer.Server.Server.Filter;
using Bouncer.Server.Server.Listener;
using Microsoft.Extensions.Logging;

namespace Bouncer.Server.Server
{
	internal sealed class ServerManager
	{
		private readonly ILogger<ServerManager> logger;
		private readonly ILogger<RegisteredServer> serverLogger;

		private readonly ConcurrentDictionary<uint, RegisteredServer> serversById;
		private readonly ConcurrentDictionary<string, RegisteredServer> serversByName;

		private readonly List<ServerStatusListener> listeners;

		private uint nextId;

		public ServerManager(ILogger<ServerManager> logger, ILogger<RegisteredServer> serverLogger)
		{
			this.logger = logger;
			this.serverLogger = serverLogger;

			this.serversById = new ConcurrentDictionary<uint, RegisteredServer>();
			this.serversByName = new ConcurrentDictionary<string, RegisteredServer>();

			this.listeners = new List<ServerStatusListener>();

			_ = this.Cleanup();
		}

		internal RegisteredServer Register(ServerData data)
		{
			uint id = Interlocked.Increment(ref this.nextId);

			RegisteredServer server = new(this.serverLogger, id, data);

			this.serversById[id] = server;
			this.serversByName.AddOrUpdate(data.Name, (_, newValue) => newValue, (_, oldValue, newValue) =>
			{
				this.Unregister(oldValue);

				return newValue;
			}, server);

			this.logger.LogDebug(EventIds.ServerRegistered, $"Registered server: {data.Name} (Id: {id}) | (Total: {this.serversById.Count})");

			lock (this.listeners)
			{
				foreach (ServerStatusListener listener in this.listeners)
				{
					listener.TryAddServer(server);
				}
			}

			return server;
		}
		
		internal void Unregister(RegisteredServer server)
		{
			if (!this.serversById.TryRemove(server.Id, out _))
			{
				return;
			}

			this.logger.LogDebug(EventIds.ServerUnregistered, $"Unregistered server: {server.Name} (Id: {server.Id}) | (Total: {this.serversById.Count})");

			//Use KVP to make sure the name and instance matches
			this.serversByName.TryRemove(new KeyValuePair<string, RegisteredServer>(server.Name, server));

			server.Unregister();
		}

		internal ServerStatusListener CreateServerListener(IServerFilter? filter)
		{
			ServerStatusListener listener = new(this, filter);

			lock (this.listeners)
			{
				this.listeners.Add(listener);
			}

			foreach (RegisteredServer server in this.serversById.Values)
			{
				listener.TryAddServer(server);
			}

			return listener;
		}

		internal void RemoveListener(ServerStatusListener listener)
		{
			lock (this.listeners)
			{
				this.listeners.Remove(listener);
			}
		}

		private async Task Cleanup()
		{
			while (true)
			{
				foreach (RegisteredServer server in this.serversById.Values)
				{
					server.Cleanup();
				}

				await Task.Delay(TimeSpan.FromSeconds(1));
			}
		}

		internal bool TryGetServer(uint id, [NotNullWhen(true)] out RegisteredServer? server) => this.serversById.TryGetValue(id, out server);

		internal ICollection<RegisteredServer> Servers => this.serversById.Values;
	}
}
