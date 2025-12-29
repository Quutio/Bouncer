package io.quut.bouncer.sponge.server

import com.google.inject.Inject
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.sponge.universe.SpongeBouncerUniverse
import io.quut.bouncer.sponge.user.SpongeUserManager

internal class SpongeBouncerServerManager @Inject constructor(networkManager: NetworkManager, userManager: SpongeUserManager)
	: AbstractServerManager<SpongeBouncerServer, SpongeBouncerUniverse>(networkManager, userManager)
{
	override fun createServer(options: IBouncerServerOptions): SpongeBouncerServer
	{
		return SpongeBouncerServer(options.info)
	}
}
