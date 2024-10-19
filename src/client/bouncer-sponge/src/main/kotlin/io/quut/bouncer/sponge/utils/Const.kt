package io.quut.bouncer.sponge.utils

import org.spongepowered.api.ResourceKey

internal object Const
{
	const val NAMESPACE: String = "bouncer"

	val LOGIN_CHANNEL_KEY: ResourceKey = Const.key("login")

	fun key(value: String): ResourceKey = ResourceKey.of(Const.NAMESPACE, value)
}
