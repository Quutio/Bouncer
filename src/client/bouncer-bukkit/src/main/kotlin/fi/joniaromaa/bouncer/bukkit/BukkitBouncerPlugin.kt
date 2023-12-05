package fi.joniaromaa.bouncer.bukkit

import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.api.server.IBouncerServer
import fi.joniaromaa.bouncer.bukkit.config.PluginConfig
import fi.joniaromaa.bouncer.bukkit.listeners.PlayerListener
import fi.joniaromaa.bouncer.common.BouncerAPI
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.bukkit.entity.Player
import org.bukkit.plugin.java.JavaPlugin
import org.yaml.snakeyaml.DumperOptions
import java.io.File
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress

class BukkitBouncerPlugin : JavaPlugin()
{
	private lateinit var config: PluginConfig

	private lateinit var bouncer: BouncerAPI
	private lateinit var bouncerServer: IBouncerServer

	override fun onEnable()
	{
		this.saveDefaultConfig()
		this.loadPluginConfig()

		val info = BouncerServerInfo(
			this.config.name,
			this.config.group,
			this.config.type,

			InetSocketAddress.createUnresolved(
				System.getenv("SERVER_IP") ?: this.server.ip.ifEmpty {
					//Figure out this machines local address which can then be used to connect to this server
					DatagramSocket().use { socket ->
						socket.connect(InetAddress.getByName("1.1.1.1"), 53)

						return@use socket.localAddress.hostAddress
					}
				},
				this.server.port
			)
		)

		this.bouncer = BouncerAPI(config.apiUrl)
		this.bouncerServer = this.bouncer.serverLoadBalancer.registerServer(info)

		for (player: Player in this.server.onlinePlayers)
		{
			this.bouncerServer.confirmJoin(player.uniqueId)
		}

		this.server.pluginManager.registerEvents(PlayerListener(this.bouncerServer), this)
	}

	private fun loadPluginConfig()
	{
		val loader: YAMLConfigurationLoader = YAMLConfigurationLoader.builder()
			.setFile(File(this.dataFolder.absolutePath, "config.yml"))
			.setFlowStyle(DumperOptions.FlowStyle.BLOCK)
			.build()

		val node: ConfigurationNode = loader.load()

		this.config = PluginConfig.loadFrom(node)
	}

	override fun onDisable()
	{
		this.bouncer.shutdownGracefully()
	}
}