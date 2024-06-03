package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.harmony.api.IHarmonyScopeOptions

interface IServerManager
{
	fun registerServer(info: BouncerServerInfo, harmony: IHarmonyScopeOptions<IBouncerServer>): IBouncerServer
	fun unregisterServer(server: IBouncerServer)

	var defaultServer: IBouncerServer?

	// TODO: Comprehensive rules
	fun setFallback(scope: IBouncerScope)
}
