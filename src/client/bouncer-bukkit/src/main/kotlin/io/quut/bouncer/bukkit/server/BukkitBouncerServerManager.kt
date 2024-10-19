package io.quut.bouncer.bukkit.server

import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.bukkit.universe.BukkitBouncerUniverse
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager

internal class BukkitBouncerServerManager(networkManager: NetworkManager, userManager: UserManager) : AbstractServerManager<BukkitBouncerServer, BukkitBouncerUniverse>(networkManager, userManager)
{
	override fun createServer(options: IBouncerServerOptions): BukkitBouncerServer = BukkitBouncerServer(this, options.info)
}
