package io.quut.bouncer.velocity.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent
import com.velocitypowered.api.event.player.ServerLoginPluginMessageEvent.ResponseResult
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import java.nio.ByteBuffer
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class ServerLoginPluginListener
{
	private val reservations: ConcurrentMap<UUID, Int> = ConcurrentHashMap()

	fun addConnection(player: Player, reservationId: Int)
	{
		this.reservations[player.uniqueId] = reservationId
	}

	@Subscribe
	fun onServerLoginPluginMessage(event: ServerLoginPluginMessageEvent)
	{
		if (event.identifier != ServerLoginPluginListener.LOGIN_CHANNEL)
		{
			return
		}

		val player: Player = event.connection.player

		val reservationId: Int = this.reservations[player.uniqueId] ?: return

		event.result = ResponseResult.reply(ByteBuffer.allocate(4).putInt(reservationId).array())
	}

	@Subscribe(order = PostOrder.LATE)
	fun onDisconnect(event: DisconnectEvent)
	{
		this.reservations.remove(event.player.uniqueId)
	}

	companion object
	{
		private val LOGIN_CHANNEL: ChannelIdentifier = MinecraftChannelIdentifier.create("bouncer", "login")
	}
}
