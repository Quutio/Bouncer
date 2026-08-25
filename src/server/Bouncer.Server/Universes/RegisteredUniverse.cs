using System.Runtime.InteropServices;
using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Session;
using Google.Protobuf;
using Google.Protobuf.Collections;

namespace Bouncer.Server.Universes;

internal sealed class RegisteredUniverse(UniverseManager universeManager, BouncerSession session, RegisteredServer server, int id, UniverseData universeData, State state) : IEquatable<RegisteredUniverse>, IComparable<RegisteredUniverse>
{
	private static readonly TimeSpan Timeout = TimeSpan.FromSeconds(5);

	private readonly UniverseManager universeManager = universeManager;
	private readonly BouncerSession session = session;

	internal RegisteredServer Server { get; } = server;

	public UniverseData Data { get; set; } = universeData;
	public State State { get; set; } = state;

	internal int Id { get; } = id;

	private readonly Dictionary<Guid, long?> players = [];

	private readonly Lock stateLock = new();

	internal void Update()
	{
		this.universeManager.Update(this, this.State.Type, this.players.Count);
	}

	public Task<int?>? ReserveSlot(RepeatedField<ByteString> players)
	{
		lock (this.stateLock)
		{
			if (this.State.MaxPlayers > 0 && this.players.Count + players.Count > this.State.MaxPlayers)
			{
				return null;
			}

			foreach (Guid player in players.Select(p => new Guid(p.ToByteArray())))
			{
				ref long? value = ref CollectionsMarshal.GetValueRefOrAddDefault(this.players, player, out bool exists);
				value = Environment.TickCount64 + (long)RegisteredUniverse.Timeout.TotalMilliseconds;

				if (!exists)
				{
					this.Update();
				}
			}
		}

		return ReserveAsync();

		async Task<int?> ReserveAsync()
		{
			ClientSessionMessage.Types.ReserveResponse response = await this.session.WriteAsync(new ServerSessionMessage.Types.ReserveRequest
			{
				UniverseId = this.Id,
				Players = { players }
			}).ConfigureAwait(false);

			switch (response.ResultCase)
			{
				case ClientSessionMessage.Types.ReserveResponse.ResultOneofCase.Success:
					return response.Success.ReservationId;
			}

			return null;
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
		}
	}

	internal void Quit(Guid player)
	{
		lock (this.stateLock)
		{
			if (this.players.Remove(player, out _))
			{
				this.Update();
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
			}
		}
	}

	internal void SetState(State state)
	{
		lock (this.stateLock)
		{
			this.State = state;

			this.Update();

			this.Server.AddUpdate(new BouncerWatchResponse()
			{
				Universe = new BouncerWatchResponse.Types.Universe()
				{
					ServerId = this.Id,
					Update = new UniverseUpdate()
					{
						State = state,
					}
				}
			});
		}
	}

	internal void Unregister() => this.universeManager.Unregister(this);

	public override string ToString() => $"RegisteredUniverse {this.Id}";
	public override int GetHashCode() => this.Id;

	public override bool Equals(object? obj) => obj is RegisteredUniverse other && this.Equals(other);
	public bool Equals(RegisteredUniverse? other) => this.Id == other?.Id;

	public int CompareTo(RegisteredUniverse? other) => other is null ? 1 : this.Id.CompareTo(other.Id);
}
