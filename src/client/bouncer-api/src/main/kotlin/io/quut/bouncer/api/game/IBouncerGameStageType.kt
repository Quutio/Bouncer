package io.quut.bouncer.api.game

import java.util.Collections

interface IBouncerGameStageType<T : Any>
{
	val factory: (IBouncerGame) -> T
	val listeners: Collection<IBouncerGameStageListener<T>>

	companion object
	{
		@JvmStatic
		fun <T : Any> of(factory: (IBouncerGame) -> T, vararg listeners: IBouncerGameStageListener<T>): IBouncerGameStageType<T> =
			Impl(factory, Collections.unmodifiableCollection(listeners.toList()))
	}

	private class Impl<T : Any>(
		override val factory: (IBouncerGame) -> T,
		override val listeners: Collection<IBouncerGameStageListener<T>>) : IBouncerGameStageType<T>
}
