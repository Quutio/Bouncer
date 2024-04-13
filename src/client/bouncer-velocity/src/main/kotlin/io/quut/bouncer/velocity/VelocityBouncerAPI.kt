package io.quut.bouncer.velocity

import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.BouncerAPI

internal class VelocityBouncerAPI(private val plugin: VelocityBouncerPlugin, endpoint: String) : BouncerAPI(endpoint)
{
	override fun shutdownSignalHook()
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
