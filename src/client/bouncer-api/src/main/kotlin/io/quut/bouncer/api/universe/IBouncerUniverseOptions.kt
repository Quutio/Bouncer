package io.quut.bouncer.api.universe

interface IBouncerUniverseOptions
{
	val info: IBouncerUniverseInfo

	companion object
	{
		@JvmStatic
		fun of(info: IBouncerUniverseInfo): IBouncerUniverseOptions
		{
			return Impl(info)
		}
	}

	private class Impl(override var info: IBouncerUniverseInfo) : IBouncerUniverseOptions
}
