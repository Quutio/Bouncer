package io.quut.bouncer.bukkit

import io.quut.bouncer.bukkit.listeners.CommandListener
import io.quut.bouncer.bukkit.server.BukkitServerManager
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import org.bukkit.plugin.java.JavaPlugin
import java.net.InetSocketAddress

class BukkitBouncerPluginLoader : JavaPlugin()
{
	private val bouncer: BukkitBouncerPlugin = BukkitBouncerPlugin(this, this.server, this.dataFolder, BukkitServerManager())

	override fun onLoad()
	{
		this.saveDefaultConfig()

		this.bouncer.load()
	}

	override fun onEnable()
	{
		this.bouncer.enable(InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(this.server.ip), this.server.port))

		this.server.pluginManager.registerEvents(CommandListener(this.bouncer), this)
	}

	override fun onDisable()
	{
		this.bouncer.shutdownNow()
	}
}
