package io.quut.bouncer.api.game

import net.kyori.adventure.key.Key
import java.util.Collections

interface IBouncerGameArea
{
	companion object
	{
		@JvmStatic
		fun world(worldKey: Key): IWorld
		{
			return IWorld.of(worldKey)
		}

		fun compound(vararg area: IBouncerGameArea): ICompound
		{
			return ICompound.of(area.toHashSet())
		}
	}

	interface IWorld : IBouncerGameArea
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

	interface ICompound : IBouncerGameArea
	{
		val scopes: Collection<IBouncerGameArea>

		companion object
		{
			@JvmStatic
			fun of(scopes: Set<IBouncerGameArea>): ICompound
			{
				return Impl(Collections.unmodifiableSet(scopes))
			}
		}

		private class Impl(override val scopes: Set<IBouncerGameArea>) : ICompound
	}
}
