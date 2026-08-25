package io.quut.bouncer.bukkit

import io.quut.bouncer.bukkit.config.PluginConfig
import io.quut.bouncer.bukkit.server.BukkitDistributedServerManager
import io.quut.bouncer.bukkit.universe.BukkitDistributedUniverseManager
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.network.NetworkManager
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.configurate.yaml.NodeStyle
import org.spongepowered.configurate.yaml.YamlConfigurationLoader
import java.io.File

internal class BukkitBouncerPlugin(
	private val dataDirectory: File,
	override val universeManager: BukkitDistributedUniverseManager,
	networkManager: NetworkManager,
	serverManager: BukkitDistributedServerManager) : BouncerPlugin(networkManager, serverManager)
{
	private val configLoader: ConfigurationLoader<CommentedConfigurationNode> = YamlConfigurationLoader.builder()
		.file(this.dataDirectory.resolve("config.yml"))
		.nodeStyle(NodeStyle.BLOCK)
		.build()

	override lateinit var config: PluginConfig

	override fun loadConfig()
	{
		val node: CommentedConfigurationNode = this.configLoader.load()

		this.config = node.require(PluginConfig::class.java)

		node.set(this.config)

		this.configLoader.save(node)
	}
}
