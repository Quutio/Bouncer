package io.quut.bouncer.common.universe

import io.quut.bouncer.api.universe.IBouncerUniverse
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.common.server.BouncerServer

abstract class BouncerUniverse<TServer, TUniverse>(internal val server: TServer, internal val options: IBouncerUniverseOptions) : RegisteredBouncerScope(), IBouncerUniverse
	where TServer : BouncerServer<TServer, TUniverse>, TUniverse : BouncerUniverse<TServer, TUniverse>
{
	override val mutex: Any
		get() = this.server

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterUniverse(this, sessionData)
	}
}
