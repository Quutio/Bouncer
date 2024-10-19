package io.quut.bouncer.velocity.server

import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.server.BouncerServer
import io.quut.bouncer.velocity.universe.VelocityBouncerUniverse

internal class VelocityBouncerServer(serverManager: VelocityBouncerServerManager, info: IBouncerServerInfo) : BouncerServer<VelocityBouncerServer, VelocityBouncerUniverse>(serverManager, info)
{
	override fun createUniverse(options: IBouncerUniverseOptions): VelocityBouncerUniverse
	{
		TODO("Not yet implemented")
	}
}
