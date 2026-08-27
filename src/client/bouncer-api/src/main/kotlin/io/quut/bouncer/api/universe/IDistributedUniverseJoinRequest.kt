package io.quut.bouncer.api.universe

import net.kyori.adventure.key.Key
import java.util.UUID

interface IDistributedUniverseJoinRequest
{
	val id: Key
	val players: Collection<UUID>

	companion object
	{
		@JvmStatic
		fun of(id: Key, players: Collection<UUID>): IDistributedUniverseJoinRequest = Impl(id, players)
	}

	private class Impl(override val id: Key, override val players: Collection<UUID>) : IDistributedUniverseJoinRequest
}
