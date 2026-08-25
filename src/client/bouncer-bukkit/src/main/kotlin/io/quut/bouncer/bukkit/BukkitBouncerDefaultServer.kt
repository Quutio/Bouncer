package io.quut.bouncer.bukkit

import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerHeartbeat
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.bukkit.listeners.CommandListener
import io.quut.bouncer.bukkit.listeners.FallbackServerListener
import io.quut.bouncer.bukkit.server.BukkitDistributedServerManager
import io.quut.bouncer.common.BouncerDefaultServer
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import org.bukkit.Server
import org.bukkit.entity.Player
import java.net.InetSocketAddress

internal class BukkitBouncerDefaultServer(
	private val server: Server,
	private val loader: BukkitBouncerPluginLoader,
	private val plugin: BukkitBouncerPlugin,
	serverManager: BukkitDistributedServerManager) : BouncerDefaultServer(plugin, serverManager)
{
	private var serverContainer: IDistributedServerContainer? = null

	internal fun load()
	{
		this.installShutdownSignal()

		this.server.pluginManager.registerEvents(CommandListener(this.plugin), this.loader)

		this.serverContainer = this.createDefaultServer()
	}

	internal fun enable()
	{
		val boundAddress: InetSocketAddress = InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(this.server.ip), this.server.port)
		val address: InetSocketAddress = InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(boundAddress.hostString), boundAddress.port)

		this.serverContainer?.state = IDistributedServerState.running(address, this.server.maxPlayers)
	}

	internal fun disable()
	{
		this.serverContainer?.close()
		this.serverContainer = null
	}

	override fun defaultServerOptions(info: IDistributedServerInfo): IDistributedServerOptions =
		IDistributedServerOptions.of(info, IDistributedServerState.starting(maxPlayers = this.server.maxPlayers))

	override fun defaultServerCreated(server: IDistributedServerContainer)
	{
		for (player: Player in this.server.onlinePlayers)
		{
			server.confirmJoin(player.uniqueId)
		}

		this.server.pluginManager.registerEvents(FallbackServerListener.Accept(server), this.loader)
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.server.scheduler.runTaskTimerAsynchronously(this.loader, runnable, 20L, 20L)
	}

	override fun heartbeat(builder: IDistributedServerHeartbeat.IBuilder)
	{
		builder.tps(this.server.tps[0])
	}

	override fun onShutdownSignal()
	{
		this.server.shutdown()
	}
}
