package io.quut.bouncer.common.server

import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerHeartbeat
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.api.universe.IDistributedUniverseContainer
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceOptions
import java.util.UUID

internal class DistributedServerContainer(
	override val instance: DistributedServer,
	private val closeCallback: (DistributedServer) -> Unit) : IDistributedServerContainer
{
	override var state: IDistributedServerState
		get() = this.instance.state
		set(value)
		{
			this.instance.state = value
		}

	override fun confirmJoin(uniqueId: UUID) = this.instance.confirmJoin(uniqueId)

	override fun confirmLeave(uniqueId: UUID) = this.instance.confirmLeave(uniqueId)

	override fun heartbeat(heartbeat: IDistributedServerHeartbeat) = this.instance.heartbeat(heartbeat)

	override fun registerUniverse(options: IDistributedUniverseOptions, supervisor: IDistributedUniverseSupervisorInstanceOptions): IDistributedUniverseContainer =
		this.instance.registerUniverse(options, supervisor)

	override fun close()
	{
		this.closeCallback(this.instance)
	}
}
