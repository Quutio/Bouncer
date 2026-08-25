package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.api.universe.IDistributedUniverse
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.extensions.toUuid
import io.quut.bouncer.common.network.BiDirectionalSession
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.network.RegisteredBouncerEntity
import io.quut.bouncer.common.universe.DistributedUniverse
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.ClientSessionMessage
import io.quut.bouncer.grpc.ClientSessionMessageKt.ReserveResponseKt.success
import io.quut.bouncer.grpc.ClientSessionMessageKt.closeRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.pingRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.reserveResponse
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverRegistrationRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.serverUnregistrationRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.universeRegistration
import io.quut.bouncer.grpc.ClientSessionMessageKt.universeRegistrationRequest
import io.quut.bouncer.grpc.ClientSessionMessageKt.universeUnregistrationRequest
import io.quut.bouncer.grpc.ServerSessionMessage
import io.quut.bouncer.grpc.clientSessionMessage
import io.quut.bouncer.grpc.serverAddress
import io.quut.bouncer.grpc.serverData
import io.quut.bouncer.grpc.serverState
import io.quut.bouncer.grpc.serverStatus
import io.quut.bouncer.grpc.state
import io.quut.bouncer.grpc.universeData
import io.quut.bouncer.grpc.universeSupervisor
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

internal class ServerManagerSession(
	private val networkManager: NetworkManager,
	private val userManager: UserManager)
	: BiDirectionalSession<ClientSessionMessage, ClientSessionMessage.Builder, ServerSessionMessage>()
{
	private val nextServerTrackingId: AtomicInteger = AtomicInteger()
	private val nextUniverseTrackingId: AtomicInteger = AtomicInteger()
	private val nextReservationId: AtomicInteger = AtomicInteger()

	private val serversByServerId: ConcurrentMap<Int, DistributedServer> = ConcurrentHashMap()
	private val universesByUniverseId: ConcurrentMap<Int, DistributedUniverse> = ConcurrentHashMap()

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
			ServerSessionMessage.MessageCase.REGISTERUNIVERSERESPONSE -> this.handleCallback(message.messageId, message.registerUniverseResponse)
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
			val success: Boolean = this.writeAndForget(clientSessionMessage()
			{
				this.pingRequest = pingRequest {}
			})

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
			ServerSessionMessage.ReserveRequest.ScopeCase.UNIVERSEID ->
			{
				val universe: IDistributedUniverse = this.universesByUniverseId[reserveRequest.universeId] ?: return

				val reservationId: Int = this.nextReservationId.incrementAndGet()

				this.userManager.createReservation(reservationId, universe, reserveRequest.playersList.map(ByteString::toUuid).toSet())

				this.writeAndForget(clientSessionMessage()
				{
					this.messageId = messageId
					this.reserveResponse = reserveResponse()
					{
						this.success = success()
						{
							this.reservationId = reservationId
						}
					}
				})
			}

			else -> Unit
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	internal fun registerServer(server: DistributedServer)
	{
		val serverTrackingId: Int = this.nextServerTrackingId.incrementAndGet()

		server.prepare(this, serverTrackingId)
		{ players, universes ->
			val pendingUniverses: ArrayDeque<DistributedUniverse> = ArrayDeque()

			val job: Deferred<ServerSessionMessage.ServerRegistrationResponse> = this.writeAsync(
				ClientSessionMessage.newBuilder()
					.setRegisterServerRequest(serverRegistrationRequest()
					{
						this.trackingId = serverTrackingId
						this.data = serverData()
						{
							this.name = server.info.name
							this.group = server.info.group
							this.type = server.info.type
						}
						this.state = serverState()
						{
							this.state = state()
							{
								this.type = server.state.type.asString()
								server.state.maxPlayers?.let { m -> this.maxPlayers = m }
							}
							server.state.address?.let()
							{ a ->
								this.address = serverAddress()
								{
									this.host = a.hostString
									this.port = a.port
								}
							}
						}
						this.status = serverStatus()
						{
							if (server.info.maxMemory != null)
							{
								this.maxMemory = server.info.maxMemory!!
							}
						}

						players.forEach { player -> this.players.add(ByteString.copyFrom(player.toByteArray())) }

						universes.forEach()
						{ universe ->
							pendingUniverses.add(universe)

							val universeTrackingId: Int = this@ServerManagerSession.nextUniverseTrackingId.incrementAndGet()

							universe.prepare(this@ServerManagerSession, universeTrackingId)

							this.universes.add(this@ServerManagerSession.universeRegistration(universeTrackingId, universe))
						}
					}))

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

				response.universesList.forEach()
				{ response ->
					val universe: DistributedUniverse = pendingUniverses.removeFirst()

					val sessionData: RegisteredBouncerEntity.SessionData = universe.sessionData ?: return@forEach
					if (sessionData.session != this@ServerManagerSession)
					{
						return@forEach
					}

					if (!this@ServerManagerSession.registerUniverse(universe, sessionData.trackingId, response.universeId))
					{
						return@invokeOnCompletion
					}
				}
			}
		}
	}

	private fun registerServer(server: DistributedServer, trackingId: Int, scopeId: Int): Boolean =
		this.registerScope(this.serversByServerId, this::sendUnregisterServer, server, trackingId, scopeId)

	private fun registerUniverse(universe: DistributedUniverse, trackingId: Int, scopeId: Int): Boolean =
		this.registerScope(this.universesByUniverseId, this::sendUnregisterUniverse, universe, trackingId, scopeId)

	private fun <T : RegisteredBouncerEntity> registerScope(map: ConcurrentMap<Int, T>, unregister: (Int) -> Unit, scope: T, trackingId: Int, scopeId: Int): Boolean
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

	internal fun unregisterServer(server: DistributedServer, sessionData: RegisteredBouncerEntity.SessionData)
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
		this.writeAndForget(clientSessionMessage()
		{
			this.unregisterServerRequest = serverUnregistrationRequest()
			{
				this.trackingId = trackingId
			}
		})
	}

	internal fun sendUpdate(update: ClientSessionMessage.ServerUpdateRequest)
	{
		this.writeAndForget(clientSessionMessage()
		{
			this.updateServerRequest = update
		})
	}

	internal fun sendUpdate(update: ClientSessionMessage.UniverseUpdateRequest)
	{
		this.writeAndForget(clientSessionMessage()
		{
			this.universeUpdateRequest = update
		})
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	internal fun registerUniverse(universe: DistributedUniverse, serverSessionData: RegisteredBouncerEntity.SessionData)
	{
		val trackingId: Int = this.nextUniverseTrackingId.incrementAndGet()

		universe.prepare(this, trackingId)

		val job: Deferred<ServerSessionMessage.UniverseRegistrationResponse> = this.writeAsync(
			ClientSessionMessage.newBuilder().setRegisterUniverseRequest(
				universeRegistrationRequest()
				{
					this.serverTrackingId = serverSessionData.trackingId
					this.registration = this@ServerManagerSession.universeRegistration(trackingId, universe)
				}))

		job.invokeOnCompletion()
		{ ex ->
			if (ex != null)
			{
				return@invokeOnCompletion
			}

			val sessionData: RegisteredBouncerEntity.SessionData = universe.sessionData ?: return@invokeOnCompletion
			if (sessionData.session != this@ServerManagerSession)
			{
				return@invokeOnCompletion
			}

			val response: ServerSessionMessage.UniverseRegistrationResponse = job.getCompleted()

			this@ServerManagerSession.registerUniverse(universe, sessionData.trackingId, response.universeId)
		}
	}

	private fun universeRegistration(trackingId: Int, universe: DistributedUniverse): ClientSessionMessage.UniverseRegistration =
		universeRegistration()
		{
			this.trackingId = trackingId
			this.data = universeData()
			{
				this.type = universe.options.info.type.asString()
				this.supervisor = universeSupervisor()
				{
					this.type = universe.supervisor.info.type.asString()
					this.attributes.putAll(universe.supervisor.info.attributes)
				}
			}
			state = state()
			{
				this.type = universe.state.type.asString()
				universe.state.maxPlayers?.let { maxPlayers -> this.maxPlayers = maxPlayers }
			}
		}

	internal fun unregisterUniverse(universe: DistributedUniverse, sessionData: RegisteredBouncerEntity.SessionData)
	{
		if (!this.universesByUniverseId.remove(sessionData.scopeId, universe))
		{
			return
		}

		// NOTE: USE TRACKING ID! **NOT** UNIVERSE ID!
		this.sendUnregisterUniverse(sessionData.trackingId)
	}

	private fun sendUnregisterUniverse(trackingId: Int)
	{
		this.writeAndForget(clientSessionMessage()
		{
			this.unregisterUniverseRequest = universeUnregistrationRequest()
			{
				this.trackingId = trackingId
			}
		})
	}

	override fun prepareResponse(messageId: Int, response: ClientSessionMessage.Builder): ClientSessionMessage =
		response.setMessageId(messageId).build()

	internal fun shutdown(intentional: Boolean = false)
	{
		this.serversByServerId.values.forEach { server -> server.lostConnection() }

		this.writeAndForget(clientSessionMessage()
		{
			this.closeRequest = closeRequest()
			{
				this.intentional = intentional
			}
		})

		super.shutdown()
	}
}
