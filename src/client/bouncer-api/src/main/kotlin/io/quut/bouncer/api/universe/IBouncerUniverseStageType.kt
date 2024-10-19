package io.quut.bouncer.api.universe

import net.kyori.adventure.key.Key

interface IBouncerUniverseStageType<T : IBouncerUniverseStage<T>>
{
	val id: Key

	companion object
	{
		fun <T : IBouncerUniverseStage<T>> of(id: Key): IBouncerUniverseStageType<T> = Impl(id)
	}

	private class Impl<T : IBouncerUniverseStage<T>>(override val id: Key) : IBouncerUniverseStageType<T>
}
