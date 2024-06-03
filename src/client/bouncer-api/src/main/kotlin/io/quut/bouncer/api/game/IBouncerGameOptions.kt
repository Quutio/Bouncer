package io.quut.bouncer.api.game

interface IBouncerGameOptions
{
	val info: IBouncerGameInfo

	companion object
	{
		@JvmStatic
		fun of(info: IBouncerGameInfo): IBouncerGameOptions
		{
			return Impl(info)
		}
	}

	private class Impl(override var info: IBouncerGameInfo) : IBouncerGameOptions
}
