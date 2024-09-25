package io.quut.bouncer.bukkit

import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.api.server.IServerHeartbeat
import io.quut.bouncer.bukkit.config.PluginConfig
import io.quut.bouncer.bukkit.listeners.PlayerListener
import io.quut.bouncer.bukkit.server.BukkitServerManager
import io.quut.bouncer.common.BouncerPlugin
import ninja.leaping.configurate.ConfigurationNode
import ninja.leaping.configurate.yaml.YAMLConfigurationLoader
import org.bukkit.Server
import org.bukkit.entity.Player
import org.bukkit.plugin.Plugin
import org.yaml.snakeyaml.DumperOptions
import java.io.File

internal class BukkitBouncerPlugin(private val plugin: Plugin, private val server: Server, private val dataFolder: File, serverManager: BukkitServerManager) : BouncerPlugin(serverManager)
{
	override lateinit var config: PluginConfig

	override fun loadConfig()
	{
		val loader: YAMLConfigurationLoader = YAMLConfigurationLoader.builder()
			.setFile(File(this.dataFolder, "config.yml"))
			.setFlowStyle(DumperOptions.FlowStyle.BLOCK)
			.build()

		val node: ConfigurationNode = loader.load()

		this.config = PluginConfig.loadFrom(node)
	}

	override fun defaultServerOptions(info: IBouncerServerInfo): IBouncerServerOptions = IBouncerServerOptions.of(info)

	override fun defaultServerCreated(server: IBouncerServer)
	{
		for (player: Player in this.server.onlinePlayers)
		{
			server.confirmJoin(player.uniqueId)
		}

		this.server.pluginManager.registerEvents(PlayerListener(server), this.plugin)
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.server.scheduler.runTaskTimerAsynchronously(this.plugin, runnable, 20L, 20L)
	}

	override fun heartbeat(builder: IServerHeartbeat.IBuilder)
	{
		builder.tps(this.server.tps[0])
	}

	override fun onShutdownSignal()
	{
		this.server.shutdown()
	}
}
