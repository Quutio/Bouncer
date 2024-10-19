package io.quut.bouncer.common

import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.api.IBouncerAPI
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerHeartbeat
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.api.universe.IBouncerUniverseManager
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager
import sun.misc.Signal
import java.net.InetSocketAddress

abstract class BouncerPlugin(private val networkManager: NetworkManager, override val serverManager: AbstractServerManager<*, *>) : IBouncer
{
	override val universeManager: IBouncerUniverseManager
		get() = TODO("Not yet implemented")

	protected abstract val config: IBouncerConfig

	protected abstract fun loadConfig()
	protected abstract fun defaultServerOptions(info: IBouncerServerInfo): IBouncerServerOptions
	protected abstract fun defaultServerCreated(server: IBouncerServer)
	protected abstract fun installHeartbeat(runnable: Runnable)
	protected abstract fun heartbeat(builder: IBouncerServerHeartbeat.IBuilder)
	protected abstract fun onShutdownSignal()

	fun load()
	{
		this.serverManager.init()

		this.loadConfig()

		this.networkManager.connect(System.getenv("BOUNCER_ADDRESS") ?: this.config.apiUrl)
	}

	fun enable(address: InetSocketAddress)
	{
		IBouncerAPI.register(this)

		this.installShutdownSignal()

		val info = IBouncerServerInfo.of(
			this.config.name,
			this.config.group,
			this.config.type,
			address,
			maxMemory = (Runtime.getRuntime().maxMemory() / 1024L / 1024L).toInt())

		val server: IBouncerServer = this.serverManager.registerServer(this.defaultServerOptions(info))

		this.serverManager.defaultServer = server
		this.serverManager.fallback = server

		this.defaultServerCreated(server)

		this.installHeartbeat { server.heartbeat(this.heartbeat()) }
	}

	fun disable()
	{
		IBouncerAPI.unregister(this)

		this.serverManager.shutdown(intentional = true)
	}

	private fun heartbeat(): IBouncerServerHeartbeat
	{
		val builder: IBouncerServerHeartbeat.IBuilder = IBouncerServerHeartbeat.builder()

		this.heartbeat(builder)

		builder.memory(((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L).toInt())

		return builder.build()
	}

	private fun installShutdownSignal()
	{
		Signal.handle(Signal("INT"))
		{ _ ->
			this.shutdownGracefully()
			this.onShutdownSignal()
		}
	}

	fun shutdownGracefully()
	{
		this.serverManager.shutdown(intentional = true)
		this.networkManager.shutdown()
	}

	fun shutdownNow()
	{
		this.serverManager.shutdown()
		this.networkManager.shutdownNow()
	}
}
