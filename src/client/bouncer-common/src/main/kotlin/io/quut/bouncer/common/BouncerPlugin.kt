package io.quut.bouncer.common

import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.api.IBouncerAPI
import io.quut.bouncer.api.universe.IDistributedUniverseManager
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.server.AbstractServerManager

abstract class BouncerPlugin(private val networkManager: NetworkManager, override val serverManager: AbstractServerManager) : IBouncer
{
	override val universeManager: IDistributedUniverseManager
		get() = TODO("Not yet implemented")

	abstract val config: IBouncerConfig

	protected abstract fun loadConfig()

	fun load()
	{
		this.loadConfig()

		this.networkManager.connect(System.getenv("BOUNCER_ADDRESS") ?: this.config.apiUrl)

		IBouncerAPI.register(this)
	}

	fun shutdownGracefully()
	{
		this.serverManager.shutdown(intentional = true)
	}

	fun shutdownNow()
	{
		this.serverManager.shutdown()
		this.networkManager.shutdown()

		IBouncerAPI.unregister(this)
	}
}
