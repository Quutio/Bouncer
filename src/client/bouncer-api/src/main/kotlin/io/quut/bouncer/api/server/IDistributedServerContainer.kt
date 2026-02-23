package io.quut.bouncer.api.server

import io.quut.bouncer.api.unit.IDistributedUnitContainer
import io.quut.bouncer.api.universe.IDistributedUniverseContainer
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceOptions

interface IDistributedServerContainer : IDistributedUnitContainer
{
	val instance: IDistributedServer

	override var state: IDistributedServerState

	fun heartbeat(heartbeat: IDistributedServerHeartbeat)

	fun registerUniverse(options: IDistributedUniverseOptions, supervisor: IDistributedUniverseSupervisorInstanceOptions): IDistributedUniverseContainer
}
