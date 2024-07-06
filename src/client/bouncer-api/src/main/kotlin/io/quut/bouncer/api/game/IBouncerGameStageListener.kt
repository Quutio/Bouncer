package io.quut.bouncer.api.game

import java.lang.invoke.MethodHandles

interface IBouncerGameStageListener<T : Any>
{
	val plugin: Any

	val listener: (IBouncerGame, T) -> Any

	val lookup: MethodHandles.Lookup?

	companion object
	{
		@JvmStatic
		@JvmOverloads
		fun <T : Any> of(plugin: Any, listener: (IBouncerGame, T) -> Any, lookup: MethodHandles.Lookup? = null): IBouncerGameStageListener<T> =
			Impl(plugin, listener, lookup)
	}

	private class Impl<T : Any>(
		override val plugin: Any,
		override val listener: (IBouncerGame, T) -> Any,
		override val lookup: MethodHandles.Lookup?) : IBouncerGameStageListener<T>
}
