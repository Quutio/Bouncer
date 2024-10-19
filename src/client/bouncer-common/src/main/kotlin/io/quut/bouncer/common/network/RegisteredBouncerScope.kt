package io.quut.bouncer.common.network

import io.quut.bouncer.common.server.ServerManagerSession
import java.util.concurrent.atomic.AtomicReference

abstract class RegisteredBouncerScope
{
	private val state: AtomicReference<RegistrationState> = AtomicReference(RegistrationState.NONE)

	@Volatile
	private var _sessionData: SessionData? = null

	internal val sessionData: SessionData?
		get() = this._sessionData

	protected abstract val mutex: Any

	internal val valid
		get() = this.state.get() != RegistrationState.UNREGISTERED

	internal val registered
		get() = this.state.get() == RegistrationState.REGISTERED

	internal fun prepare(session: ServerManagerSession, trackingId: Int)
	{
		synchronized(this.mutex)
		{
			this._sessionData = SessionData(session, trackingId)
		}
	}

	internal open fun lostConnection()
	{
		this.state.set(RegistrationState.NONE)
		this._sessionData = null
	}

	internal fun registered(session: ServerManagerSession, scopeId: Int): Boolean
	{
		val sessionData: SessionData = synchronized(this.mutex)
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

		sessionData.scopeId = scopeId

		// If we successfully transition from REGISTERING -> REGISTERED then we are done
		if (this.state.compareAndSet(RegistrationState.REGISTERING, RegistrationState.REGISTERED))
		{
			return true
		}

		// If we fail to transition from REGISTERING -> REGISTERED then we were unregistered
		this._sessionData = null

		return false
	}

	internal open fun unregister()
	{
		val state: RegistrationState = this.state.getAndSet(RegistrationState.UNREGISTERED)
		if (state != RegistrationState.REGISTERED)
		{
			// If we aren't in the REGISTERED state then we shouldn't call onUnregistered.
			// This could be because we are in the progress of registration which
			// then correctly handles the cancellation by itself.
			// Or we could already be in the UNREGISTER state, and we don't need to do anything.
			return
		}

		val sessionData: SessionData = this.sessionData ?: return

		this._sessionData = null

		this.onUnregistered(sessionData)
	}

	internal abstract fun onUnregistered(sessionData: SessionData)

	internal class SessionData(val session: ServerManagerSession, val trackingId: Int, @Volatile var scopeId: Int = 0)

	private enum class RegistrationState
	{
		NONE,
		REGISTERING,
		REGISTERED,
		UNREGISTERED
	}
}
