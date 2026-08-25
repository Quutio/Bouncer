package io.quut.bouncer.sponge

import com.google.inject.Inject
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.sponge.config.PluginConfig
import io.quut.bouncer.sponge.server.SpongeDistributedServerManager
import io.quut.bouncer.sponge.universe.SpongeDistributedUniverseManager
import org.spongepowered.configurate.CommentedConfigurationNode

internal class SpongeBouncerPlugin @Inject constructor(
	private val plugin: SpongeBouncerPluginInfo,
	override val universeManager: SpongeDistributedUniverseManager,
	networkManager: NetworkManager,
	serverManager: SpongeDistributedServerManager) : BouncerPlugin(networkManager, serverManager)
{
	override lateinit var config: PluginConfig

	override fun loadConfig()
	{
		val node: CommentedConfigurationNode = this.plugin.configLoader.load()

		this.config = node.require(PluginConfig::class.java)

		node.set(this.config)

		this.plugin.configLoader.save(node)
	}
}
