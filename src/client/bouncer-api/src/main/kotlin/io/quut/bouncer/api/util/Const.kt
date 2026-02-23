package io.quut.bouncer.api.util

import net.kyori.adventure.key.Key

internal object Const
{
	const val NAMESPACE: String = "bouncer"

	fun key(value: String): Key = Key.key(this.NAMESPACE, value)
}
