package io.quut.bouncer.velocity

import com.velocitypowered.api.proxy.ProxyServer
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.api.server.IServerHeartbeat
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.velocity.server.VelocityServerManager
import java.time.Duration

internal class VelocityBouncerPlugin(private val plugin: VelocityBouncerPluginLoader, private val proxy: ProxyServer, serverManager: VelocityServerManager) : BouncerPlugin(serverManager)
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

	override fun heartbeat(builder: IServerHeartbeat.IBuilder)
	{
	}

	override fun onShutdownSignal()
	{
		this.proxy.shutdown()
	}

	override fun allServers(): Map<String, IBouncerServerInfo>
	{
		return this.plugin.serversByName.toMap()
	}

	override fun serversByGroup(group: String): Map<String, IBouncerServerInfo>
	{
		return this.plugin.serversByName.entries
			.filter { it.value.group == group }
			.associateBy({it.key}, {it.value})
	}

	override fun serverByName(name: String): IBouncerServerInfo?
	{
		return this.plugin.serversByName[name]
	}
}
