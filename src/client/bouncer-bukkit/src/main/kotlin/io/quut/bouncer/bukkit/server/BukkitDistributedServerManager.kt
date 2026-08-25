package io.quut.bouncer.bukkit.server

import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager

internal class BukkitDistributedServerManager(
	networkManager: NetworkManager,
	userManager: UserManager) : AbstractServerManager(networkManager, userManager)
