package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope

interface IServerManager
{
	var defaultServer: IBouncerServer?

	fun registerServer(options: IBouncerServerOptions): IBouncerServer
	fun unregisterServer(server: IBouncerServer)

	// TODO: Comprehensive rules
	fun setFallback(scope: IBouncerScope)
}
