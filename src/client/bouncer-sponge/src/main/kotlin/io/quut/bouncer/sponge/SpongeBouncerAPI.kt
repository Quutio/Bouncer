package io.quut.bouncer.sponge

import io.quut.bouncer.common.BouncerAPI
import org.spongepowered.api.Game

internal class SpongeBouncerAPI(private val game: Game, endpoint: String) : BouncerAPI(endpoint)
{
	override fun shutdownSignalHook()
	{
		if (this.game.isServerAvailable)
		{
			this.game.server().shutdown()
		}
	}
}
