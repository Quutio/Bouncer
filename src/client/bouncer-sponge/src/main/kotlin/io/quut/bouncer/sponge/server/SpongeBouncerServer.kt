package io.quut.bouncer.sponge.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.game.IBouncerGameOptions
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.common.game.AbstractBouncerGame
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.bouncer.sponge.game.SpongeBouncerGame
import io.quut.harmony.api.IHarmonyEventManager
import org.spongepowered.plugin.PluginContainer

class SpongeBouncerServer(private val pluginContainer: PluginContainer, serverManager: SpongeServerManager, eventManager: IHarmonyEventManager<IBouncerScope>?, info: IBouncerServerInfo) : AbstractBouncerServer(serverManager, eventManager, info)
{
	override fun createGame(options: IBouncerGameOptions): AbstractBouncerGame
	{
		return SpongeBouncerGame(this.pluginContainer, this, options.info)
	}
}
