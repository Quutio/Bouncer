package io.quut.bouncer.api.universe

interface IBouncerUniverseStage<T : IBouncerUniverseStage<T>>
{
	val type: IBouncerUniverseStageType<T>

	fun begin()
	{
	}

	fun end()
	{
	}
}
