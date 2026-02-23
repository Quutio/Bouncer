using System.Collections.Concurrent;
using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Session;

namespace Bouncer.Server.Universes;

internal sealed class UniverseManager(ServerManager serverManager)
{
	private readonly ServerManager serverManager = serverManager;

	private readonly ConcurrentDictionary<int, RegisteredUniverse> universes = [];

	private int nextId;

	internal RegisteredUniverse Register(BouncerSession session, RegisteredServer server, UniverseData universeData, State state)
	{
		int id = Interlocked.Increment(ref this.nextId);

		RegisteredUniverse universe = new(session, server, id, universeData, state);

		this.universes[id] = universe;

		server.RegisterUniverse(universe);

		return universe;
	}

	internal void Unregister(RegisteredUniverse universe)
	{
		if (!this.universes.TryRemove(universe.Id, out _))
		{
			return;
		}

		universe.Server.UnregisterUniverse(universe);
	}

	internal ICollection<RegisteredUniverse> Universes => this.universes.Values;
}
