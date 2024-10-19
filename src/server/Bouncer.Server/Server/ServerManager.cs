using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using Bouncer.Grpc;
using Bouncer.Server.Logging;
using Bouncer.Server.Server.Filter;
using Bouncer.Server.Server.Watch;
using Bouncer.Server.Session;
using Google.Protobuf;

namespace Bouncer.Server.Server;

internal sealed class ServerManager
{
	private readonly ILoggerFactory loggerFactory;
	private readonly ILogger<ServerManager> logger;

	private readonly ConcurrentDictionary<uint, RegisteredServer> serversById;
	private readonly ConcurrentDictionary<string, RegisteredServer> serversByName;

	private readonly List<ServerWatcher> watchers;

	private uint nextId;

	public ServerManager(ILoggerFactory loggerFactory, ILogger<ServerManager> logger)
	{
		this.loggerFactory = loggerFactory;
		this.logger = logger;

		this.serversById = new ConcurrentDictionary<uint, RegisteredServer>();
		this.serversByName = new ConcurrentDictionary<string, RegisteredServer>();

		this.watchers = [];

		_ = this.Cleanup();
	}

	internal RegisteredServer Register(BouncerSession session, ServerData data, ServerStatus? status)
	{
		uint id = Interlocked.Increment(ref this.nextId);

		RegisteredServer server = new(this.loggerFactory.CreateLogger<RegisteredServer>(), this, session, id, data);
		if (status is not null)
		{
			server.Status.Tps = status.Tps;
			server.Status.Memory = status.Memory;
			server.Status.MaxMemory = status.MaxMemory;

			switch (status.PlayersCase)
			{
				case ServerStatus.PlayersOneofCase.PlayerList:
				{
					foreach (ByteString player in status.PlayerList.Players)
					{
						server.Join(new Guid(player.Span, bigEndian: true));
					}

					break;
				}

				case ServerStatus.PlayersOneofCase.PlayerListHumanReadable:
				{
					foreach (string player in status.PlayerListHumanReadable.Players)
					{
						server.Join(new Guid(player));
					}

					break;
				}
			}
		}

		this.serversById[id] = server;
		this.serversByName.AddOrUpdate(data.Name, static (_, newValue) => newValue, static (_, oldValue, newValue) =>
		{
			oldValue.Unregister();

			return newValue;
		}, server);

		this.logger.ServerRegistered(data.Name, id, this.serversById.Count);

		lock (this.watchers)
		{
			foreach (ServerWatcher watcher in this.watchers)
			{
				watcher.TryAddServer(server);
			}
		}

		return server;
	}

	internal void Unregister(RegisteredServer server, bool unregistration = false)
	{
		if (!this.serversById.TryRemove(server.Id, out _))
		{
			return;
		}

		this.logger.ServerUnregistered(server.Name, server.Id, this.serversById.Count);

		//Use KVP to make sure the name and instance matches
		//This may fail if server registered with same name and we are unregistering the old one, we can ignore this
		this.serversByName.TryRemove(new KeyValuePair<string, RegisteredServer>(server.Name, server));

		server.UnregisterInternal(unregistration);
	}

	internal ServerWatcher CreateServerWatcher(IServerFilter? filter)
	{
		ServerWatcher watcher = new(this, filter);

		lock (this.watchers)
		{
			this.watchers.Add(watcher);
		}

		foreach (RegisteredServer server in this.serversById.Values)
		{
			watcher.TryAddServer(server);
		}

		return watcher;
	}

	internal void RemoveWatcher(ServerWatcher watcher)
	{
		lock (this.watchers)
		{
			this.watchers.Remove(watcher);
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

			await Task.Delay(TimeSpan.FromSeconds(1)).ConfigureAwait(false);
		}
	}

	internal bool TryGetServer(uint id, [NotNullWhen(true)] out RegisteredServer? server) => this.serversById.TryGetValue(id, out server);

	internal ICollection<RegisteredServer> Servers => this.serversById.Values;
}
