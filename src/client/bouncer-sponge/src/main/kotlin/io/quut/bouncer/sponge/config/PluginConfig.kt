package io.quut.bouncer.sponge.config

import io.quut.bouncer.common.config.IBouncerConfig
import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
internal class PluginConfig : IBouncerConfig
{
	override var name: String = "test"
	override var group: String = "survival"
	override var type: String = "vanilla"
	override var apiUrl: String = "localhost:5000"
	var fallback: Boolean = true
}
