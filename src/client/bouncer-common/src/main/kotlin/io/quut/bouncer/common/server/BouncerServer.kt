package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.grpc.serverHeartbeat
import io.quut.bouncer.grpc.serverStatusUpdateRequest
import io.quut.bouncer.grpc.serverStatusUserJoin
import io.quut.bouncer.grpc.serverStatusUserQuit
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class BouncerServer(internal val info: BouncerServerInfo) :
	IBouncerServer
{
	private val state: AtomicReference<RegistrationState> = AtomicReference(RegistrationState.NONE)
	private val players: MutableSet<UUID> = mutableSetOf()

	private lateinit var session: ServerManagerSession

	@Volatile
	internal var id: Int = 0
		private set

	internal val registered
		get() = this.state.get() == RegistrationState.REGISTERED

	internal fun registered(session: ServerManagerSession, id: Int): Boolean
	{
		// If we fail to transition from NONE -> REGISTERING then we might already be unregistering
		if (!this.state.compareAndSet(RegistrationState.NONE, RegistrationState.REGISTERING))
		{
			return false
		}

		this.session = session
		this.id = id

		synchronized(this.players)
		{
			for (uuid: UUID in this.players)
			{
				this.sendConfirmJoin(uuid)
			}
		}

		// If we successfully to transition from REGISTERING -> REGISTERED then we are done
		if (this.state.compareAndSet(RegistrationState.REGISTERING, RegistrationState.REGISTERED))
		{
			return true
		}

		return false
	}

	internal fun lostConnection()
	{
		this.state.set(RegistrationState.NONE)
	}

	private fun updateTransaction(uniqueId: UUID, state: Boolean)
	{
		synchronized(this.players)
		{
			if (state)
			{
				if (!this.players.add(uniqueId))
				{
					return
				}

				if (this.state.get() == RegistrationState.REGISTERED)
				{
					this.sendConfirmJoin(uniqueId)
				}
			}
			else
			{
				if (!this.players.remove(uniqueId))
				{
					return
				}

				if (this.state.get() == RegistrationState.REGISTERED)
				{
					this.sendConfirmLeave(uniqueId)
				}
			}
		}
	}

	override fun confirmJoin(uniqueId: UUID) = this.updateTransaction(uniqueId, true)
	override fun confirmLeave(uniqueId: UUID) = this.updateTransaction(uniqueId, false)
	override fun heartbeat(tps: Int?, memory: Int?)
	{
		if (this.state.get() != RegistrationState.REGISTERED)
		{
			return
		}

		this.session.sendUpdate(
			serverStatusUpdateRequest()
			{
				this.serverId = this@BouncerServer.id
				this.heartbeat = serverHeartbeat()
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
		this.session.sendUpdate(
			serverStatusUpdateRequest()
			{
				this.serverId = this@BouncerServer.id
				this.userJoin = serverStatusUserJoin()
				{
					this.user = ByteString.copyFrom(uniqueId.toByteArray())
				}
			}
		)
	}

	private fun sendConfirmLeave(uniqueId: UUID)
	{
		this.session.sendUpdate(
			serverStatusUpdateRequest()
			{
				this.serverId = this@BouncerServer.id
				this.userQuit = serverStatusUserQuit()
				{
					this.user = ByteString.copyFrom(uniqueId.toByteArray())
				}
			}
		)
	}

	internal fun unregister(): Boolean
	{
		val state: RegistrationState = this.state.getAndSet(RegistrationState.UNREGISTERED)	// The registration hasn't even started yet
		if (state == RegistrationState.NONE)
		{
			return false
		}

		// If we aren't in the REGISTERED state then we shouldn't call unregister
		// This could be because we are in the progress of registration which
		// then correctly handles the cancellation by itself
		// Or we could already be in the UNREGISTER state and don't need to do anything
		if (state != RegistrationState.REGISTERED)
		{
			return false
		}

		return true
	}

	private enum class RegistrationState
	{
		NONE,
		REGISTERING,
		REGISTERED,
		UNREGISTERED
	}
}
