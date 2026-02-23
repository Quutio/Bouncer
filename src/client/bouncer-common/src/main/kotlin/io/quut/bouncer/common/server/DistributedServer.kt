package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.server.IDistributedServer
import io.quut.bouncer.api.server.IDistributedServerHeartbeat
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.api.universe.IDistributedUniverse
import io.quut.bouncer.api.universe.IDistributedUniverseContainer
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceOptions
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.network.RegisteredBouncerUnit
import io.quut.bouncer.common.universe.DistributedUniverse
import io.quut.bouncer.common.universe.DistributedUniverseContainer
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverUpdateRequest
import io.quut.bouncer.grpc.ServerUpdate
import io.quut.bouncer.grpc.serverAddress
import io.quut.bouncer.grpc.serverState
import io.quut.bouncer.grpc.serverStatus
import io.quut.bouncer.grpc.serverUpdate
import io.quut.bouncer.grpc.state
import java.util.UUID

internal class DistributedServer(override val info: IDistributedServerInfo, state: IDistributedServerState) : RegisteredBouncerUnit(), IDistributedServer
{
	private val players: MutableSet<UUID> = hashSetOf()
	private val universes: MutableSet<DistributedUniverse> = hashSetOf()

	internal var state: IDistributedServerState = state
		set(value)
		{
			field = value

			this.sendUpdate(serverUpdate()
			{
				state = serverState()
				{
					this.state = state()
					{
						this.type = value.type.asString()
						value.maxPlayers?.let { m -> this.maxPlayers = m }
					}
					value.address?.let()
					{ a ->
						this.address = serverAddress()
						{
							this.host = a.hostString
							this.port = a.port
						}
					}
				}
			})
		}

	override val mutex: Any
		get() = this

	internal fun prepare(session: ServerManagerSession, trackingId: Int, consumer: (Set<UUID>, Set<DistributedUniverse>) -> Unit)
	{
		if (!this.valid)
		{
			return
		}

		synchronized(this.mutex)
		{
			consumer(this.players, this.universes)

			super.prepare(session, trackingId)
		}
	}

	override fun lostConnection()
	{
		synchronized(this.mutex)
		{
			super.lostConnection()

			this.universes.forEach { universe -> universe.lostConnection() }
		}
	}

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

	internal fun confirmJoin(uniqueId: UUID) = this.updateTransaction(uniqueId, true)
	internal fun confirmLeave(uniqueId: UUID) = this.updateTransaction(uniqueId, false)

	internal fun heartbeat(heartbeat: IDistributedServerHeartbeat)
	{
		this.sendUpdate(
			serverUpdate()
			{
				status = serverStatus()
				{
					heartbeat.tps?.let { tps -> this.tps = (tps * 100).toInt() }
					heartbeat.memory?.let { memory -> this.memory = memory}
				}
			}
		)
	}

	private fun sendConfirmJoin(uniqueId: UUID)
	{
		this.sendUpdate(
			serverUpdate()
			{
				this.playersJoined.add(ByteString.copyFrom(uniqueId.toByteArray()))
			}
		)
	}

	private fun sendConfirmLeave(uniqueId: UUID)
	{
		this.sendUpdate(
			serverUpdate()
			{
				this.playersLeft.add(ByteString.copyFrom(uniqueId.toByteArray()))
			}
		)
	}

	private fun sendUpdate(update: ServerUpdate)
	{
		val sessionData: SessionData = this.sessionData ?: return

		sessionData.session.sendUpdate(
			serverUpdateRequest()
			{
				this.trackingId = sessionData.trackingId
				this.update = update
			}
		)
	}

	private fun createUniverse(options: IDistributedUniverseOptions, supervisor: IDistributedUniverseSupervisorInstanceOptions): DistributedUniverse =
		DistributedUniverse(this, options, supervisor)

	internal fun registerUniverse(options: IDistributedUniverseOptions, supervisor: IDistributedUniverseSupervisorInstanceOptions): IDistributedUniverseContainer
	{
		val universe: DistributedUniverse = this.createUniverse(options, supervisor)

		synchronized(this.mutex)
		{
			this.universes.add(universe)

			this.sessionData?.let()
			{ sessionData ->
				sessionData.session.registerUniverse(universe, sessionData)
			}
		}

		return DistributedUniverseContainer(universe)
	}

	internal fun unregisterUniverse(universe: IDistributedUniverse)
	{
		this.unregisterUniverse(universe as DistributedUniverse)
	}

	private fun unregisterUniverse(universe: DistributedUniverse)
	{
		synchronized(this.mutex)
		{
			if (!this.universes.remove(universe))
			{
				return
			}

			universe.unregister()
		}
	}

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterServer(this, sessionData)
	}
}
