using System.Collections.Concurrent;
using Bouncer.Grpc;
using Bouncer.Server.Logging;
using Bouncer.Server.Server.Listener;

namespace Bouncer.Server.Server;

internal sealed class RegisteredServer : IEquatable<RegisteredServer>
{
	private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(5);

	private readonly ServerManager serverManager;

	private readonly ILogger<RegisteredServer> logger;

	public uint Id { get; }
	public string Name { get; } //Immutable, don't use the one from ServerData

	public ServerData Data { get; set; }
	public ServerStatus Status { get; set; }

	private readonly CancellationTokenSource cancellationTokenSource;

	private readonly ConcurrentDictionary<Guid, long?> players;

	internal bool Unregistration { get; private set; }

	internal RegisteredServer(ServerManager serverManager, ILogger<RegisteredServer> logger, uint id, ServerData data)
	{
		this.serverManager = serverManager;

		this.logger = logger;

		this.Id = id;
		this.Name = data.Name;

		this.Data = data;
		this.Status = new ServerStatus();

		this.cancellationTokenSource = new CancellationTokenSource();

		this.players = new ConcurrentDictionary<Guid, long?>();
	}

	internal CancellationToken CancellationToken => this.cancellationTokenSource.Token;

	internal void Join(Guid player)
	{
		this.players[player] = null;

		this.logger.PlayerJoinServer(player, this.Name, this.Id, this.players.Count);
	}

	internal void Quit(Guid player)
	{
		this.players.TryRemove(player, out _);

		this.logger.PlayerQuitServer(player, this.Name, this.Id, this.players.Count);
	}

	internal void ReserveSlot(Guid player)
	{
		this.players.TryAdd(player, Environment.TickCount64 + (long)RegisteredServer.Timeout.TotalMilliseconds);

		this.logger.PlayerReserveSlotServer(player, this.Name, this.Id, this.players.Count);
	}

	internal void Cleanup()
	{
		long time = Environment.TickCount64;

		//TODO: Maybe optimize this so we only loop reserved slot players, not everyone
		foreach (KeyValuePair<Guid, long?> kvp in this.players)
		{
			long? timeout = kvp.Value;
			if (timeout is null || time < timeout)
			{
				continue;
			}

			//Use KVP so the key and value matches
			if (!this.players.TryRemove(kvp))
			{
				continue;
			}

			this.logger.PlayerReserveSlotTimeoutServer(kvp.Key, this.Name, this.Id, this.players.Count);
		}
	}

	internal CancellationTokenRegistration AddListener(ServerStatusListener listener)
	{
		return this.cancellationTokenSource.Token.UnsafeRegister(state =>
		{
			((ServerStatusListener)state!).RemoveServer(this);
		}, listener);
	}

	internal void Unregister() => this.serverManager.Unregister(this);

	internal void UnregisterInternal(bool unregistration = false)
	{
		if (unregistration)
		{
			this.Unregistration = unregistration;
		}

		this.cancellationTokenSource.Cancel();
	}

	public ICollection<Guid> Players => this.players.Keys;

	public override int GetHashCode() => (int)this.Id;

	public override bool Equals(object? obj) => obj is RegisteredServer other && this.Equals(other);
	public bool Equals(RegisteredServer? other) => this.Id == other?.Id;
}
