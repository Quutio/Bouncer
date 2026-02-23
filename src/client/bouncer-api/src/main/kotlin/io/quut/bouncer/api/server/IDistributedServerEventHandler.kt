package io.quut.bouncer.api.server

import io.quut.bouncer.api.unit.IDistributedUnitState
import io.quut.bouncer.api.universe.IDistributedUniverseInfo
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo

interface IDistributedServerEventHandler
{
	fun addServer(id: Int, info: IDistributedServerInfo, state: IDistributedServerState)
	fun updateServer(id: Int, info: IDistributedServerInfo, state: IDistributedServerState)
	fun removeServer(id: Int, info: IDistributedServerInfo, state: IDistributedServerState, reason: RemoveReason)

	fun addUniverse(id: Int, info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedUnitState)
	fun updateUniverse(id: Int, info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedUnitState)
	fun removeUniverse(id: Int, info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedUnitState)

	enum class RemoveReason
	{
		UNSPECIFIED,
		UNREGISTER,
		TIMEOUT,
		ERROR
	}
}
