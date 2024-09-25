package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.extensions.toUuid
import io.quut.bouncer.common.game.AbstractBouncerGame
import io.quut.bouncer.common.network.BiDirectionalSession
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.ClientSessionMessage
import io.quut.bouncer.grpc.ClientSessionMessage.GameRegistration
import io.quut.bouncer.grpc.ClientSessionMessageKt.ReserveResponseKt.success
import io.quut.bouncer.grpc.ClientSessionMessageKt.closeRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.gameRegistration
import io.quut.bouncer.grpc.ClientSessionMessageKt.gameRegistrationRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.gameUnregistrationRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.pingRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.reserveResponse
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverRegistrationRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverUnregistrationRequest
import io.quut.bouncer.grpc.ServerSessionMessage
import io.quut.bouncer.grpc.clientSessionMessage
import io.quut.bouncer.grpc.gameData
import io.quut.bouncer.grpc.playerList
import io.quut.bouncer.grpc.serverData
import io.quut.bouncer.grpc.serverStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

internal class ServerManagerSession(private val serverManager: AbstractServerManager, private val networkManager: NetworkManager)
	: BiDirectionalSession<ClientSessionMessage, ClientSessionMessage.Builder, ServerSessionMessage>()
{
	private val nextServerTrackingId: AtomicInteger = AtomicInteger()
	private val serversByServerId: ConcurrentMap<Int, AbstractBouncerServer> = ConcurrentHashMap()

	private val nextGameTrackingId: AtomicInteger = AtomicInteger()
	private val gamesByGameId: ConcurrentMap<Int, AbstractBouncerGame> = ConcurrentHashMap()

	private var pingTask: Job? = null

	private lateinit var stub: BouncerGrpcKt.BouncerCoroutineStub

	internal suspend fun startAsync()
	{
		try
		{
			this.stub = this.networkManager.stub

			super.startAsync(this.stub::session)
		}
		finally
		{
			this.pingTask?.cancel()
			this.pingTask = null
		}
	}

	override fun handle(message: ServerSessionMessage)
	{
		when (message.messageCase)
		{
			ServerSessionMessage.MessageCase.SETTINGSRESPONSE -> this.setSettings(message.settingsResponse)
			ServerSessionMessage.MessageCase.RESERVEREQUEST -> this.reserveRequest(message.messageId, message.reserveRequest)
			ServerSessionMessage.MessageCase.REGISTERSERVERRESPONSE -> this.handleCallback(message.messageId, message.registerServerResponse)
			ServerSessionMessage.MessageCase.REGISTERGAMERESPONSE -> this.handleCallback(message.messageId, message.registerGameResponse)

			else -> Unit
		}
	}

	private fun setSettings(settings: ServerSessionMessage.SettingsResponse)
	{
		this.pingTask?.cancel()

		@OptIn(DelicateCoroutinesApi::class) // Background task
		this.pingTask = GlobalScope.launch()
		{
			this@ServerManagerSession.pingAsync(this, Duration.ofSeconds(settings.pingInterval.seconds))
		}
	}

	private suspend fun pingAsync(scope: CoroutineScope, interval: Duration)
	{
		while (scope.isActive)
		{
			val success: Boolean = this.writeAndForget(
				clientSessionMessage()
				{
					this.pingRequest = pingRequest {}
				}
			)

			if (!success)
			{
				break
			}

			delay(interval)
		}
	}

	private fun reserveRequest(messageId: Int, reserveRequest: ServerSessionMessage.ReserveRequest)
	{
		when (reserveRequest.scopeCase)
		{
			ServerSessionMessage.ReserveRequest.ScopeCase.GAMEID ->
			{
				val game: IBouncerGame = this.gamesByGameId[reserveRequest.gameId] ?: return

				reserveRequest.playersList.forEach { player -> this.serverManager.userManager.createReservation(game, toUuid(player)) }

				this.writeAndForget(
					clientSessionMessage()
					{
						this.messageId = messageId
						this.reserveResponse = reserveResponse()
						{
							success = success {}
						}
					}
				)
			}

			else -> Unit
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	internal fun registerServer(server: AbstractBouncerServer)
	{
		val serverTrackingId: Int = this.nextServerTrackingId.incrementAndGet()

		server.prepare(this, serverTrackingId)
		{ players, games ->
			val pendingGames: ArrayDeque<AbstractBouncerGame> = ArrayDeque()

			val job: Deferred<ServerSessionMessage.ServerRegistrationResponse> = this.writeAsync(
				ClientSessionMessage.newBuilder()
				.setRegisterServerRequest(
					serverRegistrationRequest()
					{
						this.trackingId = serverTrackingId
						this.data = serverData()
						{
							this.name = server.info.name
							this.group = server.info.group
							this.type = server.info.type
							this.host = server.info.address.hostString
							this.port = server.info.address.port
						}
						this.status = serverStatus()
						{
							this.playerList = playerList()
							{
								players.forEach { player -> this.players.add(ByteString.copyFrom(player.toByteArray())) }
							}
							if (server.info.maxMemory != null)
							{
								this.maxMemory = server.info.maxMemory!!
							}
						}

						games.forEach()
						{ game ->
							pendingGames.add(game)

							val gameTrackingId: Int = this@ServerManagerSession.nextGameTrackingId.incrementAndGet()

							game.prepare(this@ServerManagerSession, gameTrackingId)

							this.games.add(this@ServerManagerSession.gameRegistration(gameTrackingId, game))
						}
					}
				)
			)

			job.invokeOnCompletion()
			{ ex ->
				if (ex != null)
				{
					return@invokeOnCompletion
				}

				val response: ServerSessionMessage.ServerRegistrationResponse = job.getCompleted()

				if (!this@ServerManagerSession.registerServer(server, serverTrackingId, response.serverId))
				{
					return@invokeOnCompletion
				}

				response.gamesList.forEach()
				{ response ->
					val game: AbstractBouncerGame = pendingGames.removeFirst()

					val sessionData: RegisteredBouncerScope.SessionData = game.sessionData ?: return@forEach
					if (sessionData.session != this@ServerManagerSession)
					{
						return@forEach
					}

					if (!this@ServerManagerSession.registerGame(game, sessionData.trackingId, response.gameId))
					{
						return@invokeOnCompletion
					}
				}
			}
		}
	}

	private fun registerServer(server: AbstractBouncerServer, trackingId: Int, scopeId: Int): Boolean
	{
		return this.registerScope(this.serversByServerId, this::sendUnregisterServer, server, trackingId, scopeId)
	}

	private fun registerGame(game: AbstractBouncerGame, trackingId: Int, scopeId: Int): Boolean
	{
		return this.registerScope(this.gamesByGameId, this::sendUnregisterGame, game, trackingId, scopeId)
	}

	private fun <T : RegisteredBouncerScope> registerScope(map: ConcurrentMap<Int, T>, unregister: (Int) -> Unit, scope: T, trackingId: Int, scopeId: Int): Boolean
	{
		if (!scope.registered(this, scopeId))
		{
			// Also send the unregistration, so it gets out of the load balancer queue
			// NOTE: USE TRACKING ID! **NOT** SCOPE ID!
			unregister(trackingId)
			return false
		}

		map[scopeId] = scope

		if (!scope.registered && map.remove(scopeId, scope))
		{
			// NOTE: USE TRACKING ID! **NOT** SCOPE ID!
			unregister(trackingId)
			return false
		}

		return true
	}

	internal fun unregisterServer(server: AbstractBouncerServer, sessionData: RegisteredBouncerScope.SessionData)
	{
		if (!this.serversByServerId.remove(sessionData.scopeId, server))
		{
			return
		}

		// NOTE: USE TRACKING ID! **NOT** SERVER ID!
		this.sendUnregisterServer(sessionData.trackingId)
	}

	private fun sendUnregisterServer(trackingId: Int)
	{
		this.writeAndForget(
			clientSessionMessage()
			{
				this.unregisterServerRequest = serverUnregistrationRequest()
				{
					this.trackingId = trackingId
				}
			}
		)
	}

	internal fun sendUpdate(update: ClientSessionMessage.ServerUpdateRequest)
	{
		this.writeAndForget(
			clientSessionMessage()
			{
				this.updateServerRequest = update
			}
		)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	internal fun registerGame(game: AbstractBouncerGame, serverSessionData: RegisteredBouncerScope.SessionData)
	{
		val trackingId: Int = this.nextGameTrackingId.incrementAndGet()

		game.prepare(this, trackingId)

		val job: Deferred<ServerSessionMessage.GameRegistrationResponse> = this.writeAsync(
			ClientSessionMessage.newBuilder().setRegisterGameRequest(
				gameRegistrationRequest()
				{
					this.serverTrackingId = serverSessionData.trackingId
					this.registration = this@ServerManagerSession.gameRegistration(trackingId, game)
				}
			)
		)

		job.invokeOnCompletion()
		{ ex ->
			if (ex != null)
			{
				return@invokeOnCompletion
			}

			val sessionData: RegisteredBouncerScope.SessionData = game.sessionData ?: return@invokeOnCompletion
			if (sessionData.session != this@ServerManagerSession)
			{
				return@invokeOnCompletion
			}

			val response: ServerSessionMessage.GameRegistrationResponse = job.getCompleted()

			this@ServerManagerSession.registerGame(game, sessionData.trackingId, response.gameId)
		}
	}

	private fun gameRegistration(trackingId: Int, game: AbstractBouncerGame): GameRegistration
	{
		return gameRegistration()
		{
			this.trackingId = trackingId
			this.data = gameData()
			{
				this.gamemode = game.info.gamemode
			}
		}
	}

	internal fun unregisterGame(game: AbstractBouncerGame, sessionData: RegisteredBouncerScope.SessionData)
	{
		if (!this.gamesByGameId.remove(sessionData.scopeId, game))
		{
			return
		}

		// NOTE: USE TRACKING ID! **NOT** GAME ID!
		this.sendUnregisterGame(sessionData.trackingId)
	}

	private fun sendUnregisterGame(trackingId: Int)
	{
		this.writeAndForget(clientSessionMessage()
		{
			this.unregisterGameRequest = gameUnregistrationRequest()
			{
				this.trackingId = trackingId
			}
		})
	}

	override fun prepareResponse(messageId: Int, response: ClientSessionMessage.Builder): ClientSessionMessage
	{
		return response.setMessageId(messageId).build()
	}

	internal fun shutdown(intentional: Boolean = false)
	{
		this.serversByServerId.values.forEach { server -> server.lostConnection() }

		this.writeAndForget(
			clientSessionMessage()
			{
				this.closeRequest = closeRequest()
				{
					this.intentional = intentional
				}
			}
		)

		super.shutdown()
	}
}
