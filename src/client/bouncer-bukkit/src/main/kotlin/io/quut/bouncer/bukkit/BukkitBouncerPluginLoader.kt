package io.quut.bouncer.bukkit

import io.quut.bouncer.bukkit.listeners.CommandListener
import io.quut.bouncer.bukkit.server.BukkitBouncerServerManager
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import org.bukkit.plugin.java.JavaPlugin
import java.net.InetSocketAddress

class BukkitBouncerPluginLoader : JavaPlugin()
{
	private val userManager = UserManager()
	private val networkManager = NetworkManager()
	private val bouncer: BukkitBouncerPlugin = BukkitBouncerPlugin(this, this.server, this.dataFolder, this.networkManager, BukkitBouncerServerManager(this.networkManager, this.userManager))

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
