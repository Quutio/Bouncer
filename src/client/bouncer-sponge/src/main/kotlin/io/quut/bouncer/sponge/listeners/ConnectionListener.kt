package io.quut.bouncer.sponge.listeners

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.common.user.UserManager
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import java.util.UUID

internal class ConnectionListener(private val userManager: UserManager, private val serverManager: AbstractServerManager)
{
	@Listener(order = Order.PRE)
	private fun onAuth(event: ServerSideConnectionEvent.Auth)
	{
		this.userManager.createUserData(event.profile().uniqueId(), this.serverManager.fallback)
	}

	@Listener(order = Order.PRE)
	private fun onHandshake(event: ServerSideConnectionEvent.Handshake)
	{
		this.handleConnectionProgress(event.profile().uniqueId())
	}

	@Listener(order = Order.PRE)
	private fun onConfiguration(event: ServerSideConnectionEvent.Configuration)
	{
		this.handleConnectionProgress(event.profile().uniqueId())
	}

	private fun handleConnectionProgress(uniqueId: UUID): IBouncerScope?
	{
		val userData: UserManager.UserData = this.userManager.getUser(uniqueId) ?: return null

		return userData.scope
	}

	@Listener(order = Order.POST)
	private fun onDisconnect(event: ServerSideConnectionEvent.Disconnect): IBouncerScope?
	{
		return event.profile().map { profile -> this.userManager.userDisconnected(profile.uniqueId())?.scope }.orElse(null)
	}
}
