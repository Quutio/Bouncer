package io.quut.bouncer.api.server

interface IDistributedServerOptions
{
	val info: IDistributedServerInfo
	val state: IDistributedServerState

	companion object
	{
		@JvmStatic
		fun of(info: IDistributedServerInfo, state: IDistributedServerState): IDistributedServerOptions =
			Impl(info, state)
	}

	private class Impl(override val info: IDistributedServerInfo, override val state: IDistributedServerState) : IDistributedServerOptions
}
