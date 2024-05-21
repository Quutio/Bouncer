package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.grpc.BouncerSessionRequestKt.serverUpdate
import io.quut.bouncer.grpc.serverStatusUpdate
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class BouncerServer(internal val info: BouncerServerInfo) : IBouncerServer
{
	private val state: AtomicReference<RegistrationState> = AtomicReference(RegistrationState.NONE)
	private val players: MutableSet<UUID> = mutableSetOf()

	@Volatile
	private var sessionData: SessionData? = null

	internal val registered
		get() = this.state.get() == RegistrationState.REGISTERED

	internal fun prepare(session: ServerManagerSession, trackingId: Int, consumer: (Set<UUID>) -> Unit)
	{
		synchronized(this.players)
		{
			consumer(this.players)

			this.sessionData = SessionData(session, trackingId)
		}
	}

	internal fun registered(session: ServerManagerSession, id: Int): Boolean
	{
		val sessionData: SessionData = synchronized(this.players)
		{
			val sessionData: SessionData = this.sessionData ?: return false
			if (sessionData.session != session)
			{
				return false
			}

			return@synchronized sessionData
		}

		// If we fail to transition from NONE -> REGISTERING then we are already unregistered
		if (!this.state.compareAndSet(RegistrationState.NONE, RegistrationState.REGISTERING))
		{
			return false
		}

		sessionData.serverId = id

		// If we successfully transition from REGISTERING -> REGISTERED then we are done
		if (this.state.compareAndSet(RegistrationState.REGISTERING, RegistrationState.REGISTERED))
		{
			return true
		}

		// If we fail to transition from REGISTERING -> REGISTERED then we were unregistered
		this.sessionData = null

		return false
	}

	internal fun lostConnection()
	{
		this.state.set(RegistrationState.NONE)
		this.sessionData = null
	}

	private fun updateTransaction(uniqueId: UUID, state: Boolean)
	{
		if (this.state.get() == RegistrationState.UNREGISTERED)
		{
			return
		}

		synchronized(this.players)
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

	override fun heartbeat(tps: Int?, memory: Int?)
	{
		val sessionData: SessionData = this.sessionData ?: return

		sessionData.session.sendUpdate(
			serverUpdate()
			{
				this.trackingId = sessionData.trackingId
				this.status = serverStatusUpdate()
				{
					if (tps != null)
					{
						this.tps = tps
					}

					if (memory != null)
					{
						this.memory = memory
					}
				}
			}
		)
	}

	private fun sendConfirmJoin(uniqueId: UUID)
	{
		var sessionData: SessionData = this.sessionData ?: return

		sessionData.session.sendUpdate(
			serverUpdate()
			{
				this.trackingId = sessionData.trackingId
				this.status = serverStatusUpdate()
				{
					this.playersJoined.add(ByteString.copyFrom(uniqueId.toByteArray()))
				}
			}
		)
	}

	private fun sendConfirmLeave(uniqueId: UUID)
	{
		val sessionData: SessionData = this.sessionData ?: return

		sessionData.session.sendUpdate(
			serverUpdate()
			{
				this.trackingId = sessionData.trackingId
				this.status = serverStatusUpdate()
				{
					this.playersLeft.add(ByteString.copyFrom(uniqueId.toByteArray()))
				}
			}
		)
	}

	internal fun unregister()
	{
		val state: RegistrationState = this.state.getAndSet(RegistrationState.UNREGISTERED)
		if (state != RegistrationState.REGISTERED)
		{
			// If we aren't in the REGISTERED state then we shouldn't call unregisterServer.
			// This could be because we are in the progress of registration which
			// then correctly handles the cancellation by itself.
			// Or we could already be in the UNREGISTER state, and we don't need to do anything.
			return
		}

		val sessionData: SessionData = this.sessionData ?: return

		this.sessionData = null

		sessionData.session.unregisterServer(this, sessionData.trackingId, sessionData.serverId)
	}

	private class SessionData(val session: ServerManagerSession, val trackingId: Int, @Volatile var serverId: Int = 0)

	private enum class RegistrationState
	{
		NONE,
		REGISTERING,
		REGISTERED,
		UNREGISTERED
	}
}
