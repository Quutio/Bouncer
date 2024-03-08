package io.quut.bouncer.sponge.config

import org.spongepowered.configurate.objectmapping.ConfigSerializable

@ConfigSerializable
class PluginConfig
{
	var name: String = "test"
	var group: String = "survival"
	var type: String = "vanilla"
	var apiUrl: String = "localhost:5000"
}
