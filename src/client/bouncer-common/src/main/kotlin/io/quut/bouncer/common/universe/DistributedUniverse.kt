package io.quut.bouncer.common.universe

import io.quut.bouncer.api.unit.IDistributedUnitState
import io.quut.bouncer.api.universe.IDistributedUniverse
import io.quut.bouncer.api.universe.IDistributedUniverseInfo
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceOptions
import io.quut.bouncer.common.network.RegisteredBouncerUnit
import io.quut.bouncer.common.server.DistributedServer
import io.quut.bouncer.grpc.UniverseUpdate
import io.quut.bouncer.grpc.state
import io.quut.bouncer.grpc.universeUpdate

internal class DistributedUniverse(override val server: DistributedServer, internal val options: IDistributedUniverseOptions, internal val supervisor: IDistributedUniverseSupervisorInstanceOptions) : RegisteredBouncerUnit(), IDistributedUniverse
{
	override val info: IDistributedUniverseInfo
		get() = this.options.info

	internal var state: IDistributedUnitState = this.options.state
		set(value)
		{
			field = value

			this.sendUpdate(universeUpdate()
			{
				state = state()
				{
					this.type = value.type.asString()
					value.maxPlayers?.let { maxPlayers -> this.maxPlayers = maxPlayers }
				}
			})
		}

	override val mutex: Any
		get() = this.server

	private fun sendUpdate(update: UniverseUpdate)
	{
		val sessionData: SessionData = this.sessionData ?: return

		/*sessionData.session.sendUpdate(
			universe()
			{
				this.trackingId = sessionData.trackingId
				this.update = update
			}
		)*/
	}

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterUniverse(this, sessionData)
	}
}
