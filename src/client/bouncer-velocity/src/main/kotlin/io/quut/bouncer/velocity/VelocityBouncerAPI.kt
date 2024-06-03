package io.quut.bouncer.velocity

import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.BouncerAPI
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.velocity.server.VelocityServerManager

internal class VelocityBouncerAPI(private val plugin: VelocityBouncerPlugin, endpoint: String) : BouncerAPI(endpoint)
{
	override fun createServerManager(stub: BouncerGrpcKt.BouncerCoroutineStub): AbstractServerManager
	{
		return VelocityServerManager(stub)
	}

	override fun onShutdownSignal()
	{
		this.plugin.proxy.shutdown()
	}

	override fun allServers(): Map<String, BouncerServerInfo>
	{
		return plugin.serversByName.toMap()
	}

	override fun serversByGroup(group: String): Map<String, BouncerServerInfo>
	{
		return plugin.serversByName.entries
			.filter { it.value.group == group }
			.associateBy({it.key}, {it.value})
	}

	override fun serverByName(name: String): BouncerServerInfo?
	{
		return plugin.serversByName[name]
	}
}
