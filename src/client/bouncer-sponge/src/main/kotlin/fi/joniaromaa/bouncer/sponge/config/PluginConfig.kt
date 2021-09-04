package fi.joniaromaa.bouncer.sponge.config

import org.spongepowered.configurate.ConfigurationNode
import org.spongepowered.configurate.objectmapping.ConfigSerializable
import org.spongepowered.configurate.objectmapping.ObjectMapper
import org.spongepowered.configurate.serialize.SerializationException
import kotlin.Throws
import java.lang.ExceptionInInitializerError

@ConfigSerializable
class PluginConfig {

    var name: String = "test";
    var group: String = "survival";
    var type: String = "vanilla";
    var apiUrl: String = "localhost:5000";

    companion object {
        private var MAPPER: ObjectMapper<PluginConfig>? = null
        @Throws(SerializationException::class)
        fun loadFrom(node: ConfigurationNode?): PluginConfig {
            return MAPPER!!.load(node)
        }

        init {
            try {
                MAPPER = ObjectMapper.factory().get(
                    PluginConfig::class.java
                )
            } catch (e: SerializationException) {
                throw ExceptionInInitializerError(e)
            }
        }
    }

    fun saveTo(node: ConfigurationNode?) {
        try {
            MAPPER!!.save(this, node)
        } catch (e: SerializationException) {
            e.printStackTrace()
        }
    }
}