package io.quut.bouncer.api.server

interface IBouncerServerManager
{
	var defaultServer: IBouncerServer?

	fun registerServer(options: IBouncerServerOptions): IBouncerServer
	fun unregisterServer(server: IBouncerServer)

	fun watch(request: IBouncerServerWatchRequest): IBouncerServerWatcher
}
