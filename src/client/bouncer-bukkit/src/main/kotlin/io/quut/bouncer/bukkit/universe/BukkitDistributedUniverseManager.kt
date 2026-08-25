package io.quut.bouncer.bukkit.universe

import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.universe.DistributedUniverseManager
import io.quut.bouncer.grpc.JoinUniverseResponse

internal class BukkitDistributedUniverseManager(networkManager: NetworkManager) : DistributedUniverseManager(networkManager)
{
	override fun join(request: IDistributedUniverseJoinRequest, response: JoinUniverseResponse): Boolean
	{
		TODO("Not yet implemented")
	}
}
