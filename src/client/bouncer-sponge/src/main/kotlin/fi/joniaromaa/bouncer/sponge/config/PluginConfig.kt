package fi.joniaromaa.bouncer.sponge.config

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.SerializationException
import java.lang.ExceptionInInitializerError
import kotlin.Throws

@ConfigSerializable
class PluginConfig
{
	var name: String = "test"
	var group: String = "survival"
	var type: String = "vanilla"
	var apiUrl: String = "localhost:5000"

	companion object
	{
		private var mapper: ObjectMapper<PluginConfig>? = null

		@Throws(SerializationException::class)
		fun loadFrom(node: ConfigurationNode?): PluginConfig
		{
			return mapper!!.load(node)
		}

		init
		{
			try
			{
				mapper = ObjectMapper.factory().get(
					PluginConfig::class.java
				)
			}
			catch (e: SerializationException)
			{
				throw ExceptionInInitializerError(e)
			}
		}
	}

	fun saveTo(node: ConfigurationNode?)
	{
		try
		{
			mapper!!.save(this, node)
		}
		catch (e: SerializationException)
		{
			e.printStackTrace()
		}
	}
}
