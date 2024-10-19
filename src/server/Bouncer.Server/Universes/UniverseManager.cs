using System.Collections.Concurrent;
using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Session;

namespace Bouncer.Server.Universes;

internal sealed class UniverseManager(ServerManager serverManager)
{
	private readonly ServerManager serverManager = serverManager;

	private readonly ConcurrentDictionary<uint, RegisteredUniverse> universes = [];

	private uint nextId;

	internal RegisteredUniverse Register(BouncerSession session, RegisteredServer server, UniverseData universeData)
	{
		uint id = Interlocked.Increment(ref this.nextId);

		RegisteredUniverse universe = new(session, server, id, universeData);

		this.universes[id] = universe;

		return universe;
	}

	internal void Unregister(RegisteredUniverse universe)
	{
		if (!this.universes.TryRemove(universe.Id, out _))
		{
			return;
		}
	}

	internal ICollection<RegisteredUniverse> Universes => this.universes.Values;
}
