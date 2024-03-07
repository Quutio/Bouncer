package io.quut.bouncer.velocity

import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.BouncerAPI

class VelocityBouncerAPI(val plugin: VelocityBouncerPlugin, endpoint: String) : BouncerAPI(endpoint)
{
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
