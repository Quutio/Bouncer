package io.quut.bouncer.sponge.listeners

import com.google.inject.Inject
import com.google.inject.name.Named
import io.quut.bouncer.sponge.user.SpongeUserManager
import io.quut.bouncer.sponge.utils.Const
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.network.ServerSideConnectionEvent
import org.spongepowered.api.network.channel.raw.RawDataChannel
import org.spongepowered.api.profile.GameProfile
import kotlin.jvm.optionals.getOrNull

internal class ConnectionListener @Inject constructor(
	@param: Named(Const.HANDSHAKE_CHANNEL) private val handshakeChannel: RawDataChannel,
	private val userManager: SpongeUserManager): IBouncerListener
{
	@Listener(order = Order.PRE)
	private fun onIntent(event: ServerSideConnectionEvent.Handshake)
	{
		this.handshakeChannel.handshake().sendTo(event.connection()) { }.whenComplete()
		{ buf, ex ->
			if (ex == null)
			{
				this.userManager.establishConnection(event.connection(), buf.readInt())
			}
			else
			{
				this.userManager.establishConnection(event.connection())
			}
		}
	}

	@Listener(order = Order.PRE)
	private fun onLogin(event: ServerSideConnectionEvent.Login)
	{
		this.userManager.createUserData(event.profile().uniqueId(), this.userManager.getUser(event.connection()))
	}

	@Listener(order = Order.POST)
	private fun onDisconnect(event: ServerSideConnectionEvent.Disconnect)
	{
		val profile: GameProfile? = event.profile().getOrNull()

		this.userManager.userDisconnected(event.connection(), profile?.uniqueId())
	}
}
