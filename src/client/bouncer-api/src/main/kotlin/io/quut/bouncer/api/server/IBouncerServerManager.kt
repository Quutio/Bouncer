package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope

interface IBouncerServerManager
{
	var defaultServer: IBouncerServer?

	fun registerServer(options: IBouncerServerOptions): IBouncerServer
	fun unregisterServer(server: IBouncerServer)

	// TODO: Comprehensive rules
	fun setFallback(scope: IBouncerScope)

	fun watch(request: IBouncerServerWatchRequest): IBouncerServerWatcher
}
