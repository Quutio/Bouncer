package io.quut.bouncer.common.universe

import io.quut.bouncer.api.universe.IBouncerUniverseJoinRequest
import io.quut.bouncer.api.universe.IBouncerUniverseManager
import io.quut.bouncer.common.network.NetworkManager

class BouncerUniverseManager(private val networkManager: NetworkManager) : IBouncerUniverseManager
{
	override fun join(request: IBouncerUniverseJoinRequest)
	{
		/*this.networkManager.stub.joinGame(joinGameRequest()
		{
			this.gamemode = request.gamemode.toString()
			this.players.addAll(request.players.map { player -> ByteString.copyFrom(player.toByteArray()) })
		}).*/
	}
}
