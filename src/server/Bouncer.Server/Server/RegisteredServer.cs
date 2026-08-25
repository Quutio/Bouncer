using System.Runtime.InteropServices;
using Bouncer.Grpc;
using Bouncer.Server.Logging;
using Bouncer.Server.Server.Watch;
using Bouncer.Server.Session;
using Bouncer.Server.Universes;

namespace Bouncer.Server.Server;

internal sealed class RegisteredServer : IEquatable<RegisteredServer>, IComparable<RegisteredServer>
{
	private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(5);

	private readonly ILogger<RegisteredServer> logger;

	private readonly ServerManager serverManager;
	private readonly BouncerSession session;

	private readonly Lock stateLock;

	public int Id { get; }
	public string Name { get; } //Immutable, don't use the one from ServerData

	public ServerState State { get; set; }
	public ServerData Data { get; set; }
	public ServerStatus Status { get; set; }

	private readonly CancellationTokenSource cancellationTokenSource;

	private readonly Dictionary<Guid, long?> players;

	private readonly HashSet<ServerWatcher> watches;
	private readonly HashSet<RegisteredUniverse> universes;

	internal bool Unregistration { get; private set; }

	internal RegisteredServer(ILogger<RegisteredServer> logger, ServerManager serverManager, BouncerSession session, int id, ServerState state, ServerData data)
	{
		this.logger = logger;

		this.serverManager = serverManager;
		this.session = session;

		this.stateLock = new Lock();

		this.Id = id;
		this.State = state;
		this.Name = data.Name;

		this.Data = data;
		this.Status = new ServerStatus();

		this.cancellationTokenSource = new CancellationTokenSource();

		this.players = [];

		this.watches = [];
		this.universes = [];
	}

	internal CancellationToken CancellationToken => this.cancellationTokenSource.Token;

	internal void Update()
	{
		this.serverManager.Update(this, this.State.State.Type, this.Players.Count);
	}

	internal void SetState(ServerState state)
	{
		lock (this.stateLock)
		{
			this.State = state;

			this.Update();

			lock (this.watches)
			{
				foreach (ServerWatcher watcher in this.watches)
				{
					watcher.AddUpdate(new BouncerWatchResponse()
					{
						Server = new BouncerWatchResponse.Types.Server()
						{
							ServerId = this.Id,
							Update = new ServerUpdate()
							{
								State = state,
							}
						}
					});
				}
			}
		}
	}

	internal void Join(Guid player)
	{
		lock (this.stateLock)
		{
			ref long? value = ref CollectionsMarshal.GetValueRefOrAddDefault(this.players, player, out bool exists);
			if (exists && value is not null)
			{
				value = null;
			}
			else if (!exists)
			{
				this.Update();
			}
			else
			{
				return;
			}

			this.logger.PlayerJoinServer(player, this.Name, this.Id, this.players.Count);
		}
	}

	internal void Quit(Guid player)
	{
		lock (this.stateLock)
		{
			if (this.players.Remove(player, out _))
			{
				this.Update();

				this.logger.PlayerQuitServer(player, this.Name, this.Id, this.players.Count);
			}
		}
	}

	internal void RegisterUniverse(RegisteredUniverse universe)
	{
		lock (this.watches)
		{
			this.universes.Add(universe);

			foreach (ServerWatcher watcher in this.watches)
			{
				watcher.AddUpdate(new BouncerWatchResponse()
				{
					Universe = new BouncerWatchResponse.Types.Universe()
					{
						ServerId = this.Id,
						UniverseId = universe.Id,
						Add = new BouncerWatchResponse.Types.Universe.Types.Add()
						{
							Data = universe.Data,
							State = universe.State
						}
					}
				});
			}
		}
	}

	internal void UnregisterUniverse(RegisteredUniverse universe)
	{
		lock (this.watches)
		{
			this.universes.Remove(universe);

			foreach (ServerWatcher watcher in this.watches)
			{
				watcher.AddUpdate(new BouncerWatchResponse()
				{
					Universe = new BouncerWatchResponse.Types.Universe()
					{
						ServerId = this.Id,
						UniverseId = universe.Id,
						Remove = new BouncerWatchResponse.Types.Universe.Types.Remove()
					}
				});
			}
		}
	}

	internal void ReserveSlot(Guid player)
	{
		lock (this.stateLock)
		{
			ref long? value = ref CollectionsMarshal.GetValueRefOrAddDefault(this.players, player, out bool exists);
			value = Environment.TickCount64 + (long)RegisteredServer.Timeout.TotalMilliseconds;

			if (!exists)
			{
				this.Update();
			}

			this.logger.PlayerReserveSlotServer(player, this.Name, this.Id, this.players.Count);
		}
	}

	internal void AddUpdate(BouncerWatchResponse update)
	{
		lock (this.watches)
		{
			foreach (ServerWatcher watcher in this.watches)
			{
				watcher.AddUpdate(update);
			}
		}
	}

	internal void Cleanup()
	{
		lock (this.stateLock)
		{
			long time = Environment.TickCount64;

			//TODO: Maybe optimize this so we only loop reserved slot players, not everyone
			foreach ((Guid player, long? timeout) in this.players)
			{
				if (timeout is null || time < timeout)
				{
					continue;
				}

				if (!this.players.Remove(player))
				{
					continue;
				}

				this.Update();

				this.logger.PlayerReserveSlotTimeoutServer(player, this.Name, this.Id, this.players.Count);
			}
		}

		foreach (RegisteredUniverse universe in this.universes)
		{
			universe.Cleanup();
		}
	}

	internal CancellationTokenRegistration AddWatcher(ServerWatcher watcher)
	{
		lock (this.watches)
		{
			this.watches.Add(watcher);

			foreach (RegisteredUniverse universe in this.universes)
			{
				watcher.AddUpdate(new BouncerWatchResponse()
				{
					Universe = new BouncerWatchResponse.Types.Universe()
					{
						ServerId = this.Id,
						UniverseId = universe.Id,
						Add = new BouncerWatchResponse.Types.Universe.Types.Add()
						{
							Data = universe.Data,
							State = universe.State
						}
					}
				});
			}
		}

		return this.cancellationTokenSource.Token.UnsafeRegister(state =>
		{
			((ServerWatcher)state!).RemoveServer(this);
		}, watcher);
	}

	internal void Unregister() => this.serverManager.Unregister(this);

	internal void UnregisterInternal(bool unregistration = false)
	{
		if (unregistration)
		{
			this.Unregistration = unregistration;
		}

		this.cancellationTokenSource.Cancel();

		foreach (RegisteredUniverse universe in this.universes)
		{
			universe.Unregister();
		}
	}

	public ICollection<Guid> Players => this.players.Keys;

	public override string ToString() => $"RegisteredServer {this.Id} - {this.Name}";
	public override int GetHashCode() => this.Id;

	public override bool Equals(object? obj) => obj is RegisteredServer other && this.Equals(other);
	public bool Equals(RegisteredServer? other) => this.Id == other?.Id;

	public int CompareTo(RegisteredServer? other) => other is null ? 1 : this.Id.CompareTo(other.Id);
}
