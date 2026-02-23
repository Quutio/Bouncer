package io.quut.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.plugin.PluginContainer
import com.velocitypowered.api.proxy.ProxyServer
import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerHeartbeat
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.common.BouncerDefaultServer
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import io.quut.bouncer.velocity.server.VelocityDistributedServerManager
import java.net.InetSocketAddress
import java.time.Duration

internal class VelocityBouncerDefaultServer @Inject constructor(
	private val container: PluginContainer,
	private val proxy: ProxyServer,
	plugin: VelocityBouncerPlugin,
	serverManager: VelocityDistributedServerManager) : BouncerDefaultServer(plugin, serverManager)
{
	internal fun enable()
	{
		this.createDefaultServer()
	}

	override fun defaultServerOptions(info: IDistributedServerInfo): IDistributedServerOptions
	{
		val boundAddress: InetSocketAddress = this.proxy.boundAddress
		val address: InetSocketAddress = InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(boundAddress.hostString), boundAddress.port)

		return IDistributedServerOptions.of(info, IDistributedServerState.running(address))
	}

	override fun defaultServerCreated(server: IDistributedServerContainer)
	{
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.proxy.scheduler.buildTask(this.container, runnable)
			.delay(Duration.ofSeconds(1))
			.repeat(Duration.ofSeconds(1))
			.schedule()
	}

	override fun heartbeat(builder: IDistributedServerHeartbeat.IBuilder)
	{
	}

	override fun onShutdownSignal()
	{
		this.proxy.shutdown()
	}
}
