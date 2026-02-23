package io.quut.bouncer.common.universe

import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.api.universe.IDistributedUniverseManager
import io.quut.bouncer.common.network.NetworkManager

class DistributedUniverseManager(private val networkManager: NetworkManager) : IDistributedUniverseManager
{
	override fun join(request: IDistributedUniverseJoinRequest)
	{
		/*this.networkManager.stub.joinGame(joinGameRequest()
		{
			this.gamemode = request.gamemode.toString()
			this.players.addAll(request.players.map { player -> ByteString.copyFrom(player.toByteArray()) })
		}).*/
	}
}
