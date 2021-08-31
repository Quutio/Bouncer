package fi.joniaromaa.bouncer.bukkit.config

import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.objectmapping.ObjectMapper
import ninja.leaping.configurate.objectmapping.Setting
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable

@ConfigSerializable
class PluginConfig
{
	@Setting("name")
	lateinit var name: String

	@Setting("group")
	lateinit var group: String

	@Setting("type")
	lateinit var type: String

	@Setting("api-url")
	lateinit var apiUrl: String

	companion object
	{
		@JvmStatic
		private val MAPPER: ObjectMapper<PluginConfig> = ObjectMapper.forClass(PluginConfig::class.java)

		@JvmStatic
		fun loadFrom(node: ConfigurationNode): PluginConfig = PluginConfig.MAPPER.bindToNew().populate(node)
	}
}