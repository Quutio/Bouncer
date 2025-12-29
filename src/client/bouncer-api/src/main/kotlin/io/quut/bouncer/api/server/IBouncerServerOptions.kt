package io.quut.bouncer.api.server

interface IBouncerServerOptions
{
	val info: IBouncerServerInfo

	companion object
	{
		@JvmStatic
		fun of(info: IBouncerServerInfo): IBouncerServerOptions =
			Impl(info)
	}

	private class Impl(override val info: IBouncerServerInfo) : IBouncerServerOptions
}
