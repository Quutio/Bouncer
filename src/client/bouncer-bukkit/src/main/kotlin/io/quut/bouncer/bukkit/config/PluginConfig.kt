package io.quut.bouncer.bukkit.config

import io.quut.bouncer.common.config.IBouncerConfig
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMapper
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
class PluginConfig : IBouncerConfig
{
	@Setting("name")
	override lateinit var name: String

	@Setting("group")
	override lateinit var group: String

	@Setting("type")
	override lateinit var type: String

	@Setting("api-url")
	override lateinit var apiUrl: String

	companion object
	{
		@JvmStatic
		private val MAPPER: ObjectMapper<PluginConfig> = ObjectMapper.forClass(PluginConfig::class.java)

		@JvmStatic
		fun loadFrom(node: ConfigurationNode): PluginConfig = MAPPER.bindToNew().populate(node)
	}
}
