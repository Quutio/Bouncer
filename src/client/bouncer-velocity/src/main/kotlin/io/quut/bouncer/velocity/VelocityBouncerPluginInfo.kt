package io.quut.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.plugin.annotation.DataDirectory
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.nio.file.Path

internal class VelocityBouncerPluginInfo @Inject constructor(@param: DataDirectory internal val dataDirectory: Path)
{
	internal val configLoader: ConfigurationLoader<CommentedConfigurationNode> = YamlConfigurationLoader.builder()
		.path(this.dataDirectory.resolve("config.yml"))
		.nodeStyle(NodeStyle.BLOCK)
		.build()
}
