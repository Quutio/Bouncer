using System.Collections.Concurrent;
using System.Diagnostics.CodeAnalysis;
using Bouncer.Grpc;
using Bouncer.Server.Collection;
using Bouncer.Server.Logging;
using Bouncer.Server.Server.Filter;
using Bouncer.Server.Server.Sort;
using Bouncer.Server.Server.Watch;
using Bouncer.Server.Session;
using Google.Protobuf;
using Google.Protobuf.Collections;

namespace Bouncer.Server.Server;

internal sealed class ServerManager
{
	private readonly ILoggerFactory loggerFactory;
	private readonly ILogger<ServerManager> logger;

	private readonly ConcurrentDictionary<int, RegisteredServer> serversById;
	private readonly ConcurrentDictionary<string, RegisteredServer> serversByName;

	private readonly PlayerCountTracker<RegisteredServer> playerCountTracker;

	private readonly List<ServerWatcher> watchers;

	private int nextId;

	public ServerManager(ILoggerFactory loggerFactory, ILogger<ServerManager> logger)
	{
		this.loggerFactory = loggerFactory;
		this.logger = logger;

		this.serversById = new ConcurrentDictionary<int, RegisteredServer>();
		this.serversByName = new ConcurrentDictionary<string, RegisteredServer>();

		this.playerCountTracker = new PlayerCountTracker<RegisteredServer>();

		this.watchers = [];

		_ = this.Cleanup();
	}

	internal RegisteredServer Register(BouncerSession session, ServerState state, ServerData data, ServerStatus? status, RepeatedField<ByteString> players)
	{
		int id = Interlocked.Increment(ref this.nextId);

		RegisteredServer server = new(this.loggerFactory.CreateLogger<RegisteredServer>(), this, session, id, state, data);
		if (status is not null)
		{
			server.Status.Tps = status.Tps;
			server.Status.Memory = status.Memory;
			server.Status.MaxMemory = status.MaxMemory;
		}

		foreach (ByteString player in players)
		{
			server.Join(new Guid(player.Span, bigEndian: true));
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

		server.Update();

		return server;
	}

	internal void Update(RegisteredServer server, string type, int playerCount)
	{
		if (server.State.Address is not null)
		{
			this.playerCountTracker.Update(server, type, playerCount);
		}
		else
		{
			this.playerCountTracker.Remove(server);
		}
	}

	internal void Unregister(RegisteredServer server, bool unregistration = false)
	{
		if (!this.serversById.TryRemove(server.Id, out _))
		{
			return;
		}

		this.logger.ServerUnregistered(server.Name, server.Id, this.serversById.Count);

		this.playerCountTracker.Remove(server);

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

	internal RegisteredServer? Reserve(IServerFilter? filter, IServerSort? sort, RepeatedField<ByteString> players)
	{
		lock (this.playerCountTracker.Lock)
		{
			foreach (RegisteredServer server in this.playerCountTracker.Sort(sort))
			{
				if (filter is not null && !filter.Filter(server))
				{
					continue;
				}

				foreach (ByteString user in players)
				{
					server.ReserveSlot(new Guid(user.Span, bigEndian: true));
				}

				return server;
			}
		}

		return null;
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

	internal bool TryGetServer(int id, [NotNullWhen(true)] out RegisteredServer? server) => this.serversById.TryGetValue(id, out server);

	internal ICollection<RegisteredServer> Servers => this.serversById.Values;
}
