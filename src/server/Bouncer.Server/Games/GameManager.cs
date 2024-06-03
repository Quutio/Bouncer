using System.Collections.Concurrent;
using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Session;

namespace Bouncer.Server.Games;

internal sealed class GameManager(ServerManager serverManager)
{
	private readonly ServerManager serverManager = serverManager;

	private readonly ConcurrentDictionary<uint, RegisteredGame> games = [];

	private uint nextId;

	internal RegisteredGame Register(BouncerSession session, RegisteredServer server, GameData gameData)
	{
		uint id = Interlocked.Increment(ref this.nextId);

		RegisteredGame game = new(session, server, id, gameData);

		this.games[id] = game;

		return game;
	}

	internal void Unregister(RegisteredGame game)
	{
		if (!this.games.TryRemove(game.Id, out _))
		{
			return;
		}
	}

	internal ICollection<RegisteredGame> Games => this.games.Values;
}
