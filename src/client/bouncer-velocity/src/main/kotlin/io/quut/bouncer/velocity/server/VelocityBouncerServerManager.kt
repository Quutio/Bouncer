package io.quut.bouncer.velocity.server

import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.velocity.universe.VelocityBouncerUniverse

internal class VelocityBouncerServerManager(networkManager: NetworkManager, userManager: UserManager) : AbstractServerManager<VelocityBouncerServer, VelocityBouncerUniverse>(networkManager, userManager)
{
	override fun createServer(options: IBouncerServerOptions): VelocityBouncerServer = VelocityBouncerServer(this, options.info)
}
