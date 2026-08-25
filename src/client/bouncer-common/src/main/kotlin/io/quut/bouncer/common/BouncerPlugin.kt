package io.quut.bouncer.common

import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager

abstract class BouncerPlugin(
	private val networkManager: NetworkManager,
	override val serverManager: AbstractServerManager) : IBouncer
{
	abstract val config: IBouncerConfig

	protected abstract fun loadConfig()

	fun load()
	{
		this.loadConfig()

		this.networkManager.connect(System.getenv("BOUNCER_ADDRESS") ?: this.config.apiUrl)
	}

	fun shutdownGracefully()
	{
		this.serverManager.shutdown(intentional = true)
	}

	fun shutdownNow()
	{
		this.serverManager.shutdown()
		this.networkManager.shutdown()
	}
}
