package io.quut.bouncer.sponge.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyEventManager.IBuilder.Companion.mapping
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.plugin.PluginContainer
import java.util.UUID

class SpongeServerManager(private val pluginContainer: PluginContainer, stub: BouncerGrpcKt.BouncerCoroutineStub)
	: AbstractServerManager(stub)
{
	init
	{
		this.init()
	}

	override fun createEventManager(): IHarmonyEventManager<IBouncerScope>
	{
		return IHarmonyEventManager.builder<IBouncerScope>(this.pluginContainer)
			.mapping(this::onAuth)
			.mapping(this::onHandshake)
			.mapping(this::onConfiguration)
			.mapping(this::onConnectionEvent)
			.mapping(this::onDisconnect)
			.build()
	}

	override fun createServer(info: BouncerServerInfo, eventManager: IHarmonyEventManager<IBouncerScope>?): AbstractBouncerServer
	{
		return SpongeBouncerServer(eventManager, info)
	}

	private fun onAuth(event: ServerSideConnectionEvent.Auth): IBouncerScope?
	{
		return this.createUserData(event.profile().uniqueId()).scope
	}

	private fun onHandshake(event: ServerSideConnectionEvent.Handshake): IBouncerScope?
	{
		return this.handleConnectionProgress(event.profile().uniqueId())
	}

	private fun onConfiguration(event: ServerSideConnectionEvent.Configuration): IBouncerScope?
	{
		return this.handleConnectionProgress(event.profile().uniqueId())
	}

	private fun handleConnectionProgress(uniqueId: UUID): IBouncerScope?
	{
		val userData: UserData = this.getUser(uniqueId) ?: return null

		return userData.scope
	}

	private fun onConnectionEvent(event: ServerSideConnectionEvent.ProfileScoped): IBouncerScope?
	{
		return this.getUser(event.profile().uniqueId())?.scope
	}

	private fun onDisconnect(event: ServerSideConnectionEvent.Disconnect): IBouncerScope?
	{
		return event.profile().map { profile -> this.userDisconnected(profile.uniqueId())?.scope }.orElse(null)
	}
}
