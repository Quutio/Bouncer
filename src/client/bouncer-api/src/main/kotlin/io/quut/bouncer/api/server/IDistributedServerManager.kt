package io.quut.bouncer.api.server

interface IDistributedServerManager
{
	fun registerServer(options: IDistributedServerOptions): IDistributedServerContainer

	fun watch(request: IDistributedServerWatchRequest): IDistributedServerWatcher
}
