package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerHeartbeat
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.universe.IBouncerUniverse
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.common.universe.BouncerUniverse
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverUpdateRequest
import io.quut.bouncer.grpc.ServerStatusUpdate
import io.quut.bouncer.grpc.serverStatusUpdate
import java.util.UUID

abstract class BouncerServer<TServer, TUniverse>(private val serverManager: AbstractServerManager<TServer, TUniverse>, internal val info: IBouncerServerInfo) : RegisteredBouncerScope(), IBouncerServer
	where TServer : BouncerServer<TServer, TUniverse>, TUniverse : BouncerUniverse<TServer, TUniverse>
{
	private val players: MutableSet<UUID> = hashSetOf()
	private val universes: MutableSet<TUniverse> = hashSetOf()

	override val mutex: Any
		get() = this

	internal fun prepare(session: ServerManagerSession, trackingId: Int, consumer: (Set<UUID>, Set<TUniverse>) -> Unit)
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

	override fun confirmJoin(uniqueId: UUID) = this.updateTransaction(uniqueId, true)
	override fun confirmLeave(uniqueId: UUID) = this.updateTransaction(uniqueId, false)

	override fun heartbeat(heartbeat: IBouncerServerHeartbeat)
	{
		this.sendUpdate(
			serverStatusUpdate()
			{
				heartbeat.tps?.let { tps -> this.tps = (tps * 100).toInt() }
				heartbeat.memory?.let { memory -> this.memory = memory}
			}
		)
	}

	private fun sendConfirmJoin(uniqueId: UUID)
	{
		this.sendUpdate(
			serverStatusUpdate()
			{
				this.playersJoined.add(ByteString.copyFrom(uniqueId.toByteArray()))
			}
		)
	}

	private fun sendConfirmLeave(uniqueId: UUID)
	{
		this.sendUpdate(
			serverStatusUpdate()
			{
				this.playersLeft.add(ByteString.copyFrom(uniqueId.toByteArray()))
			}
		)
	}

	private fun sendUpdate(update: ServerStatusUpdate)
	{
		val sessionData: SessionData = this.sessionData ?: return

		sessionData.session.sendUpdate(
			serverUpdateRequest()
			{
				this.trackingId = sessionData.trackingId
				this.status = update
			}
		)
	}

	protected abstract fun createUniverse(options: IBouncerUniverseOptions): TUniverse

	override fun registerUniverse(options: IBouncerUniverseOptions): IBouncerUniverse
	{
		val universe: TUniverse = this.createUniverse(options)
		universe.init()

		this.serverManager.register(universe, options.area)

		synchronized(this.mutex)
		{
			this.registerUniverse(options, universe)

			this.universes.add(universe)

			this.sessionData?.let()
			{ sessionData ->
				sessionData.session.registerUniverse(universe, sessionData)
			}
		}

		return universe
	}

	protected open fun registerUniverse(options: IBouncerUniverseOptions, universe: TUniverse)
	{
	}

	override fun unregisterUniverse(universe: IBouncerUniverse)
	{
		this.unregisterUniverse(universe as BouncerUniverse<*, *>)
	}

	private fun unregisterUniverse(universe: BouncerUniverse<*, *>)
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
