package io.quut.bouncer.api.universe

import java.security.Key
import java.util.UUID

interface IDistributedUniverseJoinRequest
{
	val id: Key
	val players: Collection<UUID>

	companion object
	{
		fun of(id: Key, players: Collection<UUID>): IDistributedUniverseJoinRequest = Impl(id, players)
	}

	private class Impl(override val id: Key, override val players: Collection<UUID>) : IDistributedUniverseJoinRequest
}
