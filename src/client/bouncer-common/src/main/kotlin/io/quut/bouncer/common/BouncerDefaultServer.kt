package io.quut.bouncer.common

import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerHeartbeat
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.server.AbstractServerManager
import sun.misc.Signal

abstract class BouncerDefaultServer(
	private val plugin: BouncerPlugin,
	private val serverManager: AbstractServerManager)
{
	protected abstract fun defaultServerOptions(info: IDistributedServerInfo): IDistributedServerOptions
	protected abstract fun defaultServerCreated(server: IDistributedServerContainer)
	protected abstract fun installHeartbeat(runnable: Runnable)
	protected abstract fun heartbeat(builder: IDistributedServerHeartbeat.IBuilder)
	protected abstract fun onShutdownSignal()

	protected fun createDefaultServer(): IDistributedServerContainer?
	{
		val defaultServer: IBouncerConfig.IDefaultServer = this.plugin.config.defaultServer
		if (!defaultServer.enabled)
		{
			return null
		}

		val info = IDistributedServerInfo.of(
			defaultServer.name,
			defaultServer.group,
			defaultServer.type,
			maxMemory = (Runtime.getRuntime().maxMemory() / 1024L / 1024L).toInt())

		val server: IDistributedServerContainer = this.serverManager.registerServer(this.defaultServerOptions(info))

		this.defaultServerCreated(server)

		this.installHeartbeat { server.heartbeat(this.heartbeat()) }

		return server
	}

	private fun heartbeat(): IDistributedServerHeartbeat
	{
		val builder: IDistributedServerHeartbeat.IBuilder = IDistributedServerHeartbeat.builder()

		this.heartbeat(builder)

		builder.memory(((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L).toInt())

		return builder.build()
	}

	protected fun installShutdownSignal()
	{
		Signal.handle(Signal("INT"))
		{ _ ->
			this.plugin.shutdownGracefully()
			this.onShutdownSignal()
		}
	}
}
