package io.quut.bouncer.sponge.server

import com.google.inject.Inject
import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.HarmonyServerManager
import io.quut.bouncer.sponge.ISpongeBouncerPlugin
import io.quut.bouncer.sponge.universe.SpongeBouncerUniverse
import io.quut.bouncer.sponge.user.SpongeUserManager
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyEventManager.IBuilder.Companion.mapping
import org.spongepowered.api.event.block.ChangeBlockEvent
import org.spongepowered.api.event.network.ServerSideConnectionEvent

internal class SpongeBouncerServerManager @Inject constructor(private val plugin: ISpongeBouncerPlugin, networkManager: NetworkManager, userManager: SpongeUserManager)
	: HarmonyServerManager<SpongeBouncerServer, SpongeBouncerUniverse>(networkManager, userManager)
{
	override fun createEventManager(): IHarmonyEventManager.IBuilder<IBouncerScope>
	{
		return IHarmonyEventManager.builder<IBouncerScope>(this.plugin.container)
			.mapping(this::onConnectionEvent)
			.mapping(this::onChangeBlockEventAll)
	}

	override fun createServer(options: IBouncerServerOptions): SpongeBouncerServer
	{
		return SpongeBouncerServer(this.plugin.container, this, options.info)
	}

	private fun onConnectionEvent(event: ServerSideConnectionEvent): IBouncerScope?
	{
		return (this.userManager as SpongeUserManager).getUser(event.connection()).scope
	}

	private fun onChangeBlockEventAll(event: ChangeBlockEvent.All): IBouncerScope?
	{
		return this.worlds[event.world().key()]
	}
}
