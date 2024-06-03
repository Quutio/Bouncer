package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameOptions
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.game.AbstractBouncerGame
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverUpdateRequest
import io.quut.bouncer.grpc.ServerStatusUpdate
import io.quut.bouncer.grpc.serverStatusUpdate
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyScopeOptions
import java.util.UUID

abstract class AbstractBouncerServer(private val eventManager: IHarmonyEventManager<IBouncerScope>?, internal val info: BouncerServerInfo)
	: RegisteredBouncerScope(), IBouncerServer
{
	private val players: MutableSet<UUID> = hashSetOf()
	private val games: MutableSet<AbstractBouncerGame> = hashSetOf()

	override val mutex: Any
		get() = this

	internal fun prepare(session: ServerManagerSession, trackingId: Int, consumer: (Set<UUID>, Set<AbstractBouncerGame>) -> Unit)
	{
		if (!this.valid)
		{
			return
		}

		synchronized(this.mutex)
		{
			consumer(this.players, this.games)

			super.prepare(session, trackingId)
		}
	}

	override fun lostConnection()
	{
		synchronized(this.mutex)
		{
			super.lostConnection()

			this.games.forEach { game -> game.lostConnection() }
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

	override fun heartbeat(tps: Int?, memory: Int?)
	{
		this.sendUpdate(
			serverStatusUpdate()
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

	protected abstract fun createGame(options: IBouncerGameOptions): AbstractBouncerGame

	override fun registerGame(options: IBouncerGameOptions, harmony: IHarmonyScopeOptions<IBouncerGame>): IBouncerGame
	{
		val game: AbstractBouncerGame = this.createGame(options)

		synchronized(this.mutex)
		{
			this.eventManager?.registerScope(game, harmony)

			this.games.add(game)

			this.sessionData?.let()
			{ sessionData ->
				sessionData.session.registerGame(game, sessionData)
			}
		}

		return game
	}

	override fun unregisterGame(game: IBouncerGame)
	{
		this.unregisterGame(game as AbstractBouncerGame)
	}

	private fun unregisterGame(game: AbstractBouncerGame)
	{
		synchronized(this.mutex)
		{
			if (!this.games.remove(game))
			{
				return
			}

			this.eventManager?.unregisterScope(game)

			game.unregister()
		}
	}

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterServer(this, sessionData)
	}
}
