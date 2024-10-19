package io.quut.bouncer.api.universe

import net.kyori.adventure.key.Key

interface IBouncerUniverseInfo
{
	val type: Key

	companion object
	{
		@JvmStatic
		fun of(type: Key): IBouncerUniverseInfo
		{
			return Impl(type)
		}
	}

	private class Impl(override val type: Key) : IBouncerUniverseInfo
}
