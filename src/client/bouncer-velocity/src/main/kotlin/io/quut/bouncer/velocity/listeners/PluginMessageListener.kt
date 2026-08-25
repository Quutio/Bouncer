package io.quut.bouncer.velocity.listeners

import com.google.common.io.ByteArrayDataInput
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.PluginMessageEvent
import com.velocitypowered.api.proxy.ServerConnection
import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.velocity.universe.VelocityDistributedUniverseManager
import io.quut.bouncer.velocity.utils.Const
import net.kyori.adventure.key.Key
import java.util.UUID

internal class PluginMessageListener(private val universeManager: VelocityDistributedUniverseManager)
{
	@Subscribe
	fun onPluginMessage(event: PluginMessageEvent)
	{
		if (event.source !is ServerConnection || event.identifier != Const.PLAY_CHANNEL_IDENTIFIER)
		{
			return
		}

		event.result = PluginMessageEvent.ForwardResult.handled()

		val data: ByteArrayDataInput = event.dataAsDataStream()

		val id: Key = Key.key(data.readUTF())
		val players: List<UUID> = List(data.readInt()) { UUID(data.readLong(), data.readLong()) }

		this.universeManager.join(IDistributedUniverseJoinRequest.of(id, players))
	}
}
