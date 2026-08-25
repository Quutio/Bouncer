package io.quut.bouncer.omnivisor.sponge.util

import org.spongepowered.api.ResourceKey

internal object Const
{
	internal const val PLUGIN_ID: String = "bouncer_omnivisor"
	internal const val NAMESPACE: String = "bouncer"

	internal val OMNIVISOR_KEY: ResourceKey = this.key("omnivisor")

	internal fun key(value: String): ResourceKey = ResourceKey.of(this.NAMESPACE, value)
}
