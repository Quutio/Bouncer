package io.quut.bouncer.sponge.listeners

import com.google.inject.Inject
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.sponge.ISpongeBouncerPlugin
import io.quut.bouncer.sponge.server.SpongeBouncerServerManager
import io.quut.bouncer.sponge.user.SpongeUserManager
import org.spongepowered.api.Game
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.api.network.ServerSideConnection
import org.spongepowered.api.profile.GameProfile
import org.spongepowered.api.scheduler.Task
import kotlin.jvm.optionals.getOrNull

internal class ConnectionListener @Inject constructor(private val game: Game, private val plugin: ISpongeBouncerPlugin, private val userManager: SpongeUserManager, private val serverManager: SpongeBouncerServerManager): IBouncerListener
{
	@Listener(order = Order.POST)
	private fun onIntent(event: ServerSideConnectionEvent.Intent)
	{
		this.plugin.loginChannel.handshake().sendTo(event.connection()) { }.whenComplete()
		{ buf, ex ->
			if (ex == null)
			{
				this.userManager.establishConnection(event.connection(), this.serverManager.fallback, buf.readInt())
			}
			else
			{
				this.userManager.establishConnection(event.connection(), this.serverManager.fallback)
			}
		}
	}

	@Listener(order = Order.PRE)
	private fun onHandshake(event: ServerSideConnectionEvent.Handshake)
	{
		this.handleConnectionProgress(event.connection())
	}

	@Listener(order = Order.PRE)
	private fun onConfiguration(event: ServerSideConnectionEvent.Configuration)
	{
		this.handleConnectionProgress(event.connection())
	}

	@Listener(order = Order.PRE)
	private fun onLogin(event: ServerSideConnectionEvent.Login)
	{
		this.userManager.createUserData(event.profile().uniqueId(), this.userManager.getUser(event.connection()))
	}

	private fun handleConnectionProgress(connection: ServerSideConnection)
	{
		val userData: UserManager.UserData = this.userManager.getUser(connection)
	}

	@Listener(order = Order.POST)
	private fun onDisconnect(event: ServerSideConnectionEvent.Disconnect)
	{
		val profile: GameProfile? = event.profile().getOrNull()

		// Remove the user after the event has been processed to allow
		// Harmony listeners to work in the POST order.
		this.game.server().scheduler().submit(Task.builder()
			.plugin(this.plugin.container)
			.execute()
			{ ->
				this.userManager.userDisconnected(event.connection(), profile?.uniqueId())
			}.build())
	}
}
