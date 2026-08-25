package io.quut.bouncer.common.universe

import com.google.protobuf.ByteString
import io.quut.bouncer.api.entity.IDistributedEntityState
import io.quut.bouncer.api.universe.IDistributedUniverse
import io.quut.bouncer.api.universe.IDistributedUniverseInfo
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceOptions
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.network.RegisteredBouncerEntity
import io.quut.bouncer.common.server.DistributedServer
import io.quut.bouncer.grpc.ClientSessionMessageKt.universeUpdateRequest
import io.quut.bouncer.grpc.UniverseUpdate
import io.quut.bouncer.grpc.state
import io.quut.bouncer.grpc.universeUpdate
import java.util.UUID

internal class DistributedUniverse(
	override val server: DistributedServer,
	internal val options: IDistributedUniverseOptions,
	internal val supervisor: IDistributedUniverseSupervisorInstanceOptions) : RegisteredBouncerEntity(), IDistributedUniverse
{
	private val players: MutableSet<UUID> = hashSetOf()

	override val info: IDistributedUniverseInfo
		get() = this.options.info

	internal var state: IDistributedEntityState = this.options.state
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

	private fun updateTransaction(uniqueId: UUID, state: Boolean)
	{
		if (!this.valid)
		{
			return
		}

		synchronized(this.mutex)
		{
			if (state)
			{
				if (!this.players.add(uniqueId))
				{
					return
				}

				this.sendConfirmJoin(uniqueId)
			}
			else
			{
				if (!this.players.remove(uniqueId))
				{
					return
				}

				this.sendConfirmLeave(uniqueId)
			}
		}
	}

	private fun sendConfirmJoin(uniqueId: UUID)
	{
		this.sendUpdate(universeUpdate()
		{
			this.playersJoined.add(ByteString.copyFrom(uniqueId.toByteArray()))
		})
	}

	private fun sendConfirmLeave(uniqueId: UUID)
	{
		this.sendUpdate(universeUpdate()
		{
			this.playersLeft.add(ByteString.copyFrom(uniqueId.toByteArray()))
		})
	}

	internal fun confirmJoin(uniqueId: UUID) = this.updateTransaction(uniqueId, true)
	internal fun confirmLeave(uniqueId: UUID) = this.updateTransaction(uniqueId, false)

	private fun sendUpdate(update: UniverseUpdate)
	{
		val sessionData: SessionData = this.sessionData ?: return

		sessionData.session.sendUpdate(universeUpdateRequest()
		{
			this.trackingId = sessionData.trackingId
			this.update = update
		})
	}

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterUniverse(this, sessionData)
	}
}
