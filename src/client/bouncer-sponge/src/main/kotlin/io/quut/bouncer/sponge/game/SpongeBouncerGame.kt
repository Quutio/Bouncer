package io.quut.bouncer.sponge.game

import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameInfo
import io.quut.bouncer.common.game.AbstractBouncerGame
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyEventManager.IBuilder.Companion.parentMapping
import org.spongepowered.plugin.PluginContainer

class SpongeBouncerGame(private val pluginContainer: PluginContainer, server: AbstractBouncerServer, info: IBouncerGameInfo) : AbstractBouncerGame(server, info)
{
	init
	{
		this.init()
	}

	override fun createEventManager(): IHarmonyEventManager<Any>
	{
		val builder = IHarmonyEventManager.builder<Any>(this.pluginContainer)
			.parentMapping { i: IBouncerGame -> (i as AbstractBouncerGame).stage }

		return builder.build()
	}
}
