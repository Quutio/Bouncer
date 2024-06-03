package io.quut.bouncer.api.game

interface IBouncerGameInfo
{
	val gamemode: String

	companion object
	{
		@JvmStatic
		fun of(gamemode: String): IBouncerGameInfo
		{
			return Impl(gamemode)
		}
	}

	private class Impl(override val gamemode: String) : IBouncerGameInfo
}
