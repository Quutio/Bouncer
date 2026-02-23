package io.quut.bouncer.api.universe.supervisor.instance

import net.kyori.adventure.key.Key

interface IDistributedUniverseSupervisorInstanceInfo
{
	val type: Key
	val attributes: Map<String, String>

	companion object
	{
		@JvmStatic
		fun of(type: Key, attributes: Map<String, String>): IDistributedUniverseSupervisorInstanceInfo = Impl(type, attributes)
	}

	private class Impl(override val type: Key, override val attributes: Map<String, String>) : IDistributedUniverseSupervisorInstanceInfo
}
