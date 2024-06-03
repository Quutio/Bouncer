package io.quut.bouncer.bukkit

import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.bukkit.config.PluginConfig
import io.quut.bouncer.bukkit.listeners.CommandListener
import io.quut.bouncer.bukkit.listeners.PlayerListener
import io.quut.bouncer.common.BouncerAPI
import io.quut.harmony.api.IHarmonyScopeOptions
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

	override fun onLoad()
	{
		this.saveDefaultConfig()
		this.loadPluginConfig()

		this.bouncer = BukkitBouncerAPI(this.server, this.config.apiUrl)
	}

	override fun onEnable()
	{
		this.bouncer.installShutdownSignal()

		val info = BouncerServerInfo(
			this.config.name,
			this.config.group,
			this.config.type,
			InetSocketAddress.createUnresolved(
				System.getenv("SERVER_IP") ?: this.server.ip.ifEmpty()
				{
					// Figure out this machines local address which can then be used to connect to this server
					DatagramSocket().use()
					{ socket ->
						socket.connect(InetAddress.getByName("1.1.1.1"), 53)

						return@use socket.localAddress.hostAddress
					}
				},
				this.server.port
			),
			maxMemory = (Runtime.getRuntime().maxMemory() / 1024L / 1024L).toInt()
		)

		this.bouncerServer = this.bouncer.serverManager.registerServer(info, IHarmonyScopeOptions.validate())

		for (player: Player in this.server.onlinePlayers)
		{
			this.bouncerServer.confirmJoin(player.uniqueId)
		}

		this.server.pluginManager.registerEvents(PlayerListener(this.bouncerServer), this)
		this.server.pluginManager.registerEvents(CommandListener(this.bouncer), this)

		this.server.scheduler.runTaskTimerAsynchronously(
			this,
			{
				this.bouncerServer.heartbeat(
					tps = (this.server.tps[0] * 100).toInt(),
					memory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L).toInt()
				)
			},
			20L,
			20L
		)
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
		this.bouncer.shutdownNow()
	}
}
