package io.quut.bouncer.api.universe

import io.quut.bouncer.api.IBouncerScope

interface IBouncerUniverse : IBouncerScope
{
	val stage: IBouncerUniverseStage<*>?

	fun <T : IBouncerUniverseStage<T>> switchStage(type: IBouncerUniverseStageType<T>)
	fun nextStage()
}
