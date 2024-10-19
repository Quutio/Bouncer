package io.quut.bouncer.api.universe

import java.lang.invoke.MethodHandles
import java.util.function.BiFunction

interface IBouncerUniverseStageListener<T : IBouncerUniverseStage<T>>
{
	val plugin: Any

	val listener: BiFunction<IBouncerUniverse, T, Any>

	val lookup: MethodHandles.Lookup?

	companion object
	{
		@JvmStatic
		@JvmOverloads
		fun <T : IBouncerUniverseStage<T>> of(plugin: Any, listener: BiFunction<IBouncerUniverse, T, Any>, lookup: MethodHandles.Lookup? = null): IBouncerUniverseStageListener<T> =
			Impl(plugin, listener, lookup)
	}

	private class Impl<T : IBouncerUniverseStage<T>>(
		override val plugin: Any,
		override val listener: BiFunction<IBouncerUniverse, T, Any>,
		override val lookup: MethodHandles.Lookup?) : IBouncerUniverseStageListener<T>
}
