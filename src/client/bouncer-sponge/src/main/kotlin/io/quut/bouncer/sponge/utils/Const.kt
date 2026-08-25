package io.quut.bouncer.sponge.utils

import org.spongepowered.api.ResourceKey

internal object Const
{
	const val NAMESPACE: String = "bouncer"

	const val HANDSHAKE_CHANNEL: String = "handshake"
	const val PLAY_CHANNEL: String = "play"

	val HANDSHAKE_CHANNEL_KEY: ResourceKey = this.key(this.HANDSHAKE_CHANNEL)
	val PLAY_CHANNEL_KEY: ResourceKey = this.key(this.PLAY_CHANNEL)

	fun key(value: String): ResourceKey = ResourceKey.of(this.NAMESPACE, value)
}
