package io.quut.bouncer.bukkit.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.game.AbstractBouncerGame
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.harmony.api.IHarmonyEventManager

class BukkitBouncerServer(eventManager: IHarmonyEventManager<IBouncerScope>?, info: BouncerServerInfo) : AbstractBouncerServer(eventManager, info)
{
	override fun createGame(): AbstractBouncerGame
	{
		TODO("Not yet implemented")
	}
}
