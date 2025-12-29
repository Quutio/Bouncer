package io.quut.bouncer.sponge.server

import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.server.BouncerServer
import io.quut.bouncer.sponge.universe.SpongeBouncerUniverse

internal class SpongeBouncerServer(info: IBouncerServerInfo) : BouncerServer<SpongeBouncerServer, SpongeBouncerUniverse>(info)
{
	override fun createUniverse(options: IBouncerUniverseOptions): SpongeBouncerUniverse
	{
		return SpongeBouncerUniverse(this, options)
	}
}
