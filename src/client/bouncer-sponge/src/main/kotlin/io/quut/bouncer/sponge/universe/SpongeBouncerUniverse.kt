package io.quut.bouncer.sponge.universe

import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.api.universe.IBouncerUniverseStage
import io.quut.bouncer.common.universe.HarmonyBouncerUniverse
import io.quut.bouncer.sponge.server.SpongeBouncerServer
import io.quut.harmony.api.IHarmonyEventManager
import org.spongepowered.plugin.PluginContainer

internal class SpongeBouncerUniverse(private val pluginContainer: PluginContainer, server: SpongeBouncerServer, options: IBouncerUniverseOptions) : HarmonyBouncerUniverse<SpongeBouncerServer, SpongeBouncerUniverse>(server, options)
{
	override fun createEventManager(): IHarmonyEventManager.IBuilder<IBouncerUniverseStage<*>> = IHarmonyEventManager.builder<IBouncerUniverseStage<*>>(this.pluginContainer)
}
