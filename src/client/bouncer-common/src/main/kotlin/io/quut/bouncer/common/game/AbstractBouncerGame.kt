package io.quut.bouncer.common.game

import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameInfo
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.common.server.AbstractBouncerServer

abstract class AbstractBouncerGame(internal val server: AbstractBouncerServer, internal val info: IBouncerGameInfo) : RegisteredBouncerScope(), IBouncerGame
{
	override val mutex: Any
		get() = this.server

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterGame(this, sessionData)
	}
}
