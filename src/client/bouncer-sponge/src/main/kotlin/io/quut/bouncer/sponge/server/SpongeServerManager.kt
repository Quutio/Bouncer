package io.quut.bouncer.sponge.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyEventManager.IBuilder.Companion.mapping
import org.spongepowered.api.event.block.ChangeBlockEvent
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.plugin.PluginContainer

class SpongeServerManager(private val pluginContainer: PluginContainer, networkManager: NetworkManager, userManager: UserManager)
	: AbstractServerManager(networkManager, userManager)
{
	init
	{
		this.init()
	}

	override fun createEventManager(): IHarmonyEventManager<IBouncerScope>
	{
		return IHarmonyEventManager.builder<IBouncerScope>(this.pluginContainer)
			.mapping(this::onConnectionEvent)
			.mapping(this::onDisconnect)
			.mapping(this::onChangeBlockEventAll)
			.build()
	}

	override fun createServer(options: IBouncerServerOptions, eventManager: IHarmonyEventManager<IBouncerScope>?): AbstractBouncerServer
	{
		return SpongeBouncerServer(this.pluginContainer, this, eventManager, options.info)
	}

	private fun onConnectionEvent(event: ServerSideConnectionEvent.ProfileScoped): IBouncerScope?
	{
		return this.userManager.getUser(event.profile().uniqueId())?.scope
	}

	private fun onDisconnect(event: ServerSideConnectionEvent.Disconnect): IBouncerScope?
	{
		return event.profile().map { profile -> this.userManager.getUser(profile.uniqueId())?.scope }.orElse(null)
	}

	private fun onChangeBlockEventAll(event: ChangeBlockEvent.All): IBouncerScope?
	{
		return this.worlds[event.world().key()]
	}
}
