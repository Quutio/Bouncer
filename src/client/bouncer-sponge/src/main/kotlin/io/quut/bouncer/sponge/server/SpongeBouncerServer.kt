package io.quut.bouncer.sponge.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.game.IBouncerGameOptions
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.game.AbstractBouncerGame
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.bouncer.sponge.game.SpongeBouncerGame
import io.quut.harmony.api.IHarmonyEventManager

class SpongeBouncerServer(eventManager: IHarmonyEventManager<IBouncerScope>?, info: BouncerServerInfo) : AbstractBouncerServer(eventManager, info)
{
	override fun createGame(options: IBouncerGameOptions): AbstractBouncerGame
	{
		return SpongeBouncerGame(this, options.info)
	}
}
