using System.Collections.Concurrent;
using Bouncer.Grpc;
using Bouncer.Server.Games.Server;
using Bouncer.Server.Server;

namespace Bouncer.Server.Games;

internal sealed class GameManager(ServerManager serverManager)
{
	private readonly ServerManager serverManager = serverManager;

	private readonly ConcurrentDictionary<uint, ServerGameManager> gamesByServer = [];

	internal void Register(uint serverId, GameData gameData)
	{
		if (!this.gamesByServer.TryGetValue(serverId, out ServerGameManager? manager))
		{
			if (!this.serverManager.TryGetServer(serverId, out RegisteredServer? server))
			{
				//There's no server registered with this id..
				return;
			}

			while (true)
			{
				manager = new ServerGameManager(server);

				if (this.gamesByServer.TryAdd(serverId, manager))
				{
					server.CancellationToken.UnsafeRegister(state =>
					{
						this.Unregister((ServerGameManager)state!);
					}, manager);

					if (server.CancellationToken.IsCancellationRequested)
					{
						return;
					}

					break;
				}
				else if (this.gamesByServer.TryGetValue(serverId, out manager))
				{
					break;
				}
			}
		}

		RegisteredGame game = new();

		manager.Register(game);
	}

	internal void Unregister(ServerGameManager manager)
	{
		if (this.gamesByServer.TryRemove(manager.Server.Id, out _))
		{
			return;
		}

		manager.Unregister();
	}
}
