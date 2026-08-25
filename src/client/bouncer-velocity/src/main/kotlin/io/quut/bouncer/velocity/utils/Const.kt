package io.quut.bouncer.velocity.utils

import com.velocitypowered.api.proxy.messages.ChannelIdentifier
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier
import net.kyori.adventure.key.Key

internal object Const
{
	const val NAMESPACE: String = "bouncer"

	internal val HANDSHAKE_CHANNEL_KEY: Key = this.key("handshake")

	internal val PLAY_CHANNEL_IDENTIFIER: ChannelIdentifier = MinecraftChannelIdentifier.from(this.key("play"))

	fun key(value: String): Key = Key.key(this.NAMESPACE, value)
}
