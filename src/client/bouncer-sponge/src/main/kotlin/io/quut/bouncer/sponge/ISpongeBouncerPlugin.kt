package io.quut.bouncer.sponge

import org.spongepowered.api.network.channel.raw.RawDataChannel
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.plugin.PluginContainer
import java.lang.invoke.MethodHandles

internal interface ISpongeBouncerPlugin
{
	val container: PluginContainer
	val lookup: MethodHandles.Lookup
	val configLoader: ConfigurationLoader<CommentedConfigurationNode>
	val loginChannel: RawDataChannel
}
