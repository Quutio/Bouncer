package io.quut.bouncer.sponge.server

import com.google.inject.Inject
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.sponge.user.SpongeUserManager

internal class SpongeDistributedServerManager @Inject constructor(
	networkManager: NetworkManager,
	userManager: SpongeUserManager): AbstractServerManager(networkManager, userManager)
