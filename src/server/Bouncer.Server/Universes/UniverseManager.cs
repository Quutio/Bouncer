using System.Collections.Concurrent;
using Bouncer.Grpc;
using Bouncer.Server.Collection;
using Bouncer.Server.Server;
using Bouncer.Server.Session;
using Google.Protobuf;
using Google.Protobuf.Collections;

namespace Bouncer.Server.Universes;

internal sealed class UniverseManager(ServerManager serverManager)
{
	private readonly ServerManager serverManager = serverManager;

	private readonly ConcurrentDictionary<int, RegisteredUniverse> universes = [];

	private readonly ConcurrentDictionary<string, PlayerCountTracker<RegisteredUniverse>> playerCountTrackers = [];

	private int nextId;

	internal RegisteredUniverse Register(BouncerSession session, RegisteredServer server, UniverseData universeData, State state, RepeatedField<ByteString> players)
	{
		int id = Interlocked.Increment(ref this.nextId);

		RegisteredUniverse universe = new(this, session, server, id, universeData, state);

		foreach (ByteString player in players)
		{
			universe.Join(new Guid(player.Span, bigEndian: true));
		}

		this.universes[id] = universe;

		server.RegisterUniverse(universe);

		universe.Update();

		return universe;
	}

	internal void Update(RegisteredUniverse universe, string type, int playerCount)
	{
		PlayerCountTracker<RegisteredUniverse> playerCountTracker = this.playerCountTrackers.GetOrAdd(universe.Data.Type, _ => new PlayerCountTracker<RegisteredUniverse>());

		if (universe.Server.State.Address is not null)
		{
			playerCountTracker.Update(universe, type, playerCount);
		}
		else
		{
			playerCountTracker.Remove(universe);
		}
	}

	internal void Unregister(RegisteredUniverse universe)
	{
		if (!this.universes.TryRemove(universe.Id, out _))
		{
			return;
		}

		PlayerCountTracker<RegisteredUniverse> playerCountTracker = this.playerCountTrackers.GetOrAdd(universe.Data.Type, _ => new PlayerCountTracker<RegisteredUniverse>());
		playerCountTracker.Remove(universe);

		universe.Server.UnregisterUniverse(universe);
	}

	internal async Task<(RegisteredUniverse Universe, int ReservationId)?> Reserve(string type, RepeatedField<ByteString> players)
	{
		PlayerCountTracker<RegisteredUniverse> playerCountTracker = this.playerCountTrackers.GetOrAdd(type, _ => new PlayerCountTracker<RegisteredUniverse>());

		while (true)
		{
			(RegisteredUniverse Universe, Task<int?> Task)? result = Find();
			if (result is not null)
			{
				int? reservationId = await result.Value.Task.ConfigureAwait(false);
				if (reservationId is not null)
				{
					return (result.Value.Universe, reservationId.Value);
				}
			}

			return null;
		}

		(RegisteredUniverse Universe, Task<int?> Task)? Find()
		{
			lock (playerCountTracker.Lock)
			{
				foreach (RegisteredUniverse universe in playerCountTracker.Sort(null))
				{
					Task<int?>? task = universe.ReserveSlot(players);
					if (task is not null)
					{
						return (universe, task);
					}
				}
			}

			return null;
		}
	}

	internal ICollection<RegisteredUniverse> Universes => this.universes.Values;
}
