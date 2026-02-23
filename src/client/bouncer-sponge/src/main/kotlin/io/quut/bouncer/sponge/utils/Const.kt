package io.quut.bouncer.sponge.utils

import org.spongepowered.api.ResourceKey

internal object Const
{
	const val NAMESPACE: String = "bouncer"

	const val LOGIN_CHANNEL: String = "login"

	val LOGIN_CHANNEL_KEY: ResourceKey = Const.key(Const.LOGIN_CHANNEL)

	fun key(value: String): ResourceKey = ResourceKey.of(Const.NAMESPACE, value)
}
