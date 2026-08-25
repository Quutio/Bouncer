package io.quut.bouncer.velocity

import com.google.inject.Inject
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.velocity.config.PluginConfig
import io.quut.bouncer.velocity.server.VelocityDistributedServerManager
import io.quut.bouncer.velocity.universe.VelocityDistributedUniverseManager
import org.spongepowered.configurate.CommentedConfigurationNode

internal class VelocityBouncerPlugin @Inject constructor(
	private val info: VelocityBouncerPluginInfo,
	override val universeManager: VelocityDistributedUniverseManager,
	networkManager: NetworkManager,
	serverManager: VelocityDistributedServerManager) : BouncerPlugin(networkManager, serverManager)
{
	override lateinit var config: IBouncerConfig

	override fun loadConfig()
	{
		val node: CommentedConfigurationNode = this.info.configLoader.load()

		this.config = node.require(PluginConfig::class.java)

		node.set(this.config)

		this.info.configLoader.save(node)
	}
}
