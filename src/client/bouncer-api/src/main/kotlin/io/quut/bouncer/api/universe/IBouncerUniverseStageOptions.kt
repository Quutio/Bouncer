package io.quut.bouncer.api.universe

import java.util.Collections
import java.util.function.Function

interface IBouncerUniverseStageOptions<T : IBouncerUniverseStage<T>>
{
	val type: IBouncerUniverseStageType<T>
	val factory: Function<IBouncerUniverse, T>
	val listeners: Collection<IBouncerUniverseStageListener<T>>

	companion object
	{
		@JvmStatic
		fun <T : IBouncerUniverseStage<T>> of(type: IBouncerUniverseStageType<T>, factory: Function<IBouncerUniverse, T>, vararg listeners: IBouncerUniverseStageListener<T>): IBouncerUniverseStageOptions<T> =
			Impl(type, factory, Collections.unmodifiableCollection(listeners.toList()))
	}

	private class Impl<T : IBouncerUniverseStage<T>>(
		override val type: IBouncerUniverseStageType<T>,
		override val factory: Function<IBouncerUniverse, T>,
		override val listeners: Collection<IBouncerUniverseStageListener<T>>) : IBouncerUniverseStageOptions<T>
}
