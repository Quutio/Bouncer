package io.quut.bouncer.api.universe

import net.kyori.adventure.key.Key
import java.util.Collections

interface IBouncerUniverseArea
{
	companion object
	{
		@JvmStatic
		fun world(worldKey: Key): IWorld
		{
			return IWorld.of(worldKey)
		}

		fun compound(vararg area: IBouncerUniverseArea): ICompound
		{
			return ICompound.of(area.toHashSet())
		}
	}

	interface IWorld : IBouncerUniverseArea
	{
		val worldKey: Key

		companion object
		{
			@JvmStatic
			fun of(worldKey: Key): IWorld
			{
				return Impl(worldKey)
			}
		}

		private class Impl(override val worldKey: Key) : IWorld
	}

	interface ICompound : IBouncerUniverseArea
	{
		val scopes: Collection<IBouncerUniverseArea>

		companion object
		{
			@JvmStatic
			fun of(scopes: Set<IBouncerUniverseArea>): ICompound
			{
				return Impl(Collections.unmodifiableSet(scopes))
			}
		}

		private class Impl(override val scopes: Set<IBouncerUniverseArea>) : ICompound
	}
}
