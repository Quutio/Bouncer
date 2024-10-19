package io.quut.bouncer.sponge.server

import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.server.HarmonyBouncerServer
import io.quut.bouncer.sponge.universe.SpongeBouncerUniverse
import org.spongepowered.plugin.PluginContainer

internal class SpongeBouncerServer(private val pluginContainer: PluginContainer, serverManager: SpongeBouncerServerManager, info: IBouncerServerInfo) : HarmonyBouncerServer<SpongeBouncerServer, SpongeBouncerUniverse>(serverManager, info)
{
	override fun createUniverse(options: IBouncerUniverseOptions): SpongeBouncerUniverse
	{
		return SpongeBouncerUniverse(this.pluginContainer, this, options)
	}
}
