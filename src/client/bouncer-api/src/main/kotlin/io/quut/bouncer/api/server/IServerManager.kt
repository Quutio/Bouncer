package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope

interface IServerManager
{
	fun registerServer(options: IBouncerServerOptions): IBouncerServer
	fun unregisterServer(server: IBouncerServer)

	var defaultServer: IBouncerServer?

	// TODO: Comprehensive rules
	fun setFallback(scope: IBouncerScope)
}
