package fi.joniaromaa.bouncer.common.server

import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.api.server.IBouncerServer
import fi.joniaromaa.bouncer.grpc.ServerStatusUpdate
import fi.joniaromaa.bouncer.grpc.ServerStatusUserJoin
import fi.joniaromaa.bouncer.grpc.ServerStatusUserQuit
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class BouncerServer(private val loadBalancer: ServerLoadBalancer, internal val info: BouncerServerInfo) : IBouncerServer
{
	private val state: AtomicReference<RegistrationState> = AtomicReference(RegistrationState.NONE)
	private val players: MutableSet<UUID> = mutableSetOf()

	@Volatile
	internal var id: Int = 0
		private set

	internal fun registered(id: Int): Boolean
	{
		// If we fail to transition from NONE -> REGISTERING then we might already be unregistering
		if (!this.state.compareAndSet(RegistrationState.NONE, RegistrationState.REGISTERING))
		{
			return false
		}

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
				this.players.add(uniqueId)
			}
			else
			{
				this.players.remove(uniqueId)
			}
		}
	}

	override fun confirmJoin(uniqueId: UUID)
	{
		this.updateTransaction(uniqueId, true)

		when (this.state.get())
		{
			RegistrationState.REGISTERED -> this.sendConfirmJoin(uniqueId)

			else -> return
		}
	}

	private fun sendConfirmJoin(uniqueId: UUID)
	{
		this.loadBalancer.sendUpdateAsync(
			ServerStatusUpdate.newBuilder()
			.setServerId(this.id)
			.setUserJoin(
				ServerStatusUserJoin.newBuilder()
				.setUser(uniqueId.toString())
				.build()
			).build()
		)
	}

	override fun confirmLeave(uniqueId: UUID)
	{
		this.updateTransaction(uniqueId, false)

		when (this.state.get())
		{
			RegistrationState.REGISTERED -> this.sendConfirmLeave(uniqueId)

			else -> return
		}
	}

	private fun sendConfirmLeave(uniqueId: UUID)
	{
		this.loadBalancer.sendUpdateAsync(
			ServerStatusUpdate.newBuilder()
			.setServerId(this.id)
			.setUserQuit(
				ServerStatusUserQuit.newBuilder()
				.setUser(uniqueId.toString())
				.build()
			).build()
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
