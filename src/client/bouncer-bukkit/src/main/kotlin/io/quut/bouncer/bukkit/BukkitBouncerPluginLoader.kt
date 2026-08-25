package io.quut.bouncer.bukkit

import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.api.IBouncerAPI
import io.quut.bouncer.bukkit.server.BukkitDistributedServerManager
import io.quut.bouncer.bukkit.universe.BukkitDistributedUniverseManager
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import org.bukkit.plugin.java.JavaPlugin

class BukkitBouncerPluginLoader : JavaPlugin()
{
	private val api: API = API()

	private val networkManager: NetworkManager = NetworkManager()
	private val userManager: UserManager = UserManager()
	private val serverManager: BukkitDistributedServerManager = BukkitDistributedServerManager(this.networkManager, this.userManager)
	private val universeManager: BukkitDistributedUniverseManager = BukkitDistributedUniverseManager(this.networkManager)
	private val plugin: BukkitBouncerPlugin = BukkitBouncerPlugin(this.dataFolder, this.universeManager, this.networkManager, this.serverManager)

	private val defaultServer: BukkitBouncerDefaultServer = BukkitBouncerDefaultServer(this.server, this, this.plugin, this.serverManager)

	override fun onLoad()
	{
		this.plugin.load()
		this.defaultServer.load()

		IBouncerAPI.register(this.api)
	}

	override fun onEnable()
	{
		this.defaultServer.enable()
	}

	override fun onDisable()
	{
		this.defaultServer.disable()
		this.plugin.shutdownNow()

		IBouncerAPI.unregister(this.api)
	}

	private inner class API : IBouncerAPI
	{
		override val bouncer: IBouncer
			get() = this@BukkitBouncerPluginLoader.plugin
	}
}
