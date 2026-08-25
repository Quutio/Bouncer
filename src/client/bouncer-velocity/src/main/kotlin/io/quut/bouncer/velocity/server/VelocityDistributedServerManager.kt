package io.quut.bouncer.velocity.server

import com.google.inject.Inject
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager

internal class VelocityDistributedServerManager @Inject constructor(
	networkManager: NetworkManager,
	userManager: UserManager) : AbstractServerManager(networkManager, userManager)
