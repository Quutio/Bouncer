package io.quut.bouncer.bukkit.server

import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.bukkit.universe.BukkitDistributedUniverse
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager

internal class BukkitDistributedServerManager(networkManager: NetworkManager, userManager: UserManager) : AbstractServerManager<BukkitDistributedServer, BukkitDistributedUniverse>(networkManager, userManager)
{
	override fun createServer(options: IDistributedServerOptions): BukkitDistributedServer = BukkitDistributedServer(options.info)
}
