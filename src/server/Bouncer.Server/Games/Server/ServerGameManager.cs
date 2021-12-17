﻿using System.Collections.Generic;
using Bouncer.Server.Server;

namespace Bouncer.Server.Games.Server;

internal sealed class ServerGameManager
{
	internal RegisteredServer Server { get; }

	private readonly Dictionary<uint, RegisteredGame> gamesById;

	internal ServerGameManager(RegisteredServer server)
	{
		this.Server = server;

		this.gamesById = new Dictionary<uint, RegisteredGame>();
	}

	internal void Register(RegisteredGame game)
	{

	}

	internal void Unregister()
	{

	}
}