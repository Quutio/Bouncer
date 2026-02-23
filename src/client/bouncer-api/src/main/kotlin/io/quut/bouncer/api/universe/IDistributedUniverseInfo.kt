package io.quut.bouncer.api.universe

import net.kyori.adventure.key.Key

interface IDistributedUniverseInfo
{
	val type: Key

	companion object
	{
		@JvmStatic
		fun of(type: Key): IDistributedUniverseInfo = Impl(type)
	}

	private class Impl(override val type: Key) : IDistributedUniverseInfo
}
