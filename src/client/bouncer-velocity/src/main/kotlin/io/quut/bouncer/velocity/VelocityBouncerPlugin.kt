package io.quut.bouncer.velocity

import com.velocitypowered.api.proxy.ProxyServer
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerHeartbeat
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.velocity.server.VelocityBouncerServerManager
import java.time.Duration

internal class VelocityBouncerPlugin(private val plugin: VelocityBouncerPluginLoader, private val proxy: ProxyServer, networkManager: NetworkManager, serverManager: VelocityBouncerServerManager) : BouncerPlugin(networkManager, serverManager)
{
	override lateinit var config: IBouncerConfig

	override fun loadConfig()
	{
		this.config = object : IBouncerConfig
		{
			override val name: String
				get() = "proxy"
			override val group: String
				get() = "proxy"
			override val type: String
				get() = "velocity"

			override val apiUrl: String
				get() = "localhost:5000"
		}
	}

	override fun defaultServerOptions(info: IBouncerServerInfo): IBouncerServerOptions = IBouncerServerOptions.of(info)

	override fun defaultServerCreated(server: IBouncerServer)
	{
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.proxy.scheduler.buildTask(this.plugin, runnable)
			.delay(Duration.ofSeconds(1))
			.repeat(Duration.ofSeconds(1))
			.schedule()
	}

	override fun heartbeat(builder: IBouncerServerHeartbeat.IBuilder)
	{
	}

	override fun onShutdownSignal()
	{
		this.proxy.shutdown()
	}
}
