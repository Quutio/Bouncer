package io.quut.bouncer.common.server

import com.google.protobuf.ByteString
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.BouncerSessionRequest
import io.quut.bouncer.grpc.BouncerSessionRequestKt.close
import io.quut.bouncer.grpc.BouncerSessionRequestKt.ping
import io.quut.bouncer.grpc.BouncerSessionRequestKt.serverRegistration
import io.quut.bouncer.grpc.BouncerSessionRequestKt.serverUnregistration
import io.quut.bouncer.grpc.BouncerSessionResponse
import io.quut.bouncer.grpc.bouncerSessionRequest
import io.quut.bouncer.grpc.playerList
import io.quut.bouncer.grpc.serverData
import io.quut.bouncer.grpc.serverStatus
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ChannelResult
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

internal class ServerManagerSession(private val stub: BouncerGrpcKt.BouncerCoroutineStub)
{
	private val requestChannel: Channel<BouncerSessionRequest> = Channel(capacity = Channel.UNLIMITED)

	private val nextRequestId: AtomicInteger = AtomicInteger()
	private val requestResponses: ConcurrentMap<Int, CompletableDeferred<*>> = ConcurrentHashMap()

	private val nextTrackingId: AtomicInteger = AtomicInteger()
	private val serversByServerId: ConcurrentMap<Int, BouncerServer> = ConcurrentHashMap()

	private var pingTask: Job? = null

	internal suspend fun start()
	{
		this@ServerManagerSession.stub.session(this@ServerManagerSession.requestChannel.receiveAsFlow())
			.cancellable()
			.collect(this::handleResponse)

		this.pingTask?.cancel()
		this.pingTask = null
	}

	@Suppress("UNCHECKED_CAST")
	private fun handleResponse(response: BouncerSessionResponse)
	{
		if (response.responseCase == BouncerSessionResponse.ResponseCase.SETTINGS)
		{
			return this.setSettings(response.settings)
		}

		val callback: CompletableDeferred<*> = this@ServerManagerSession.requestResponses.remove(response.requestId) ?: return
		when (response.responseCase)
		{
			BouncerSessionResponse.ResponseCase.SERVERREGISTRATION ->
				(callback as CompletableDeferred<BouncerSessionResponse.ServerRegistration>).complete(response.serverRegistration)

			else -> Unit
		}
	}

	private fun setSettings(settings: BouncerSessionResponse.Settings)
	{
		this.pingTask?.cancel()

		@OptIn(DelicateCoroutinesApi::class) // Background task
		this.pingTask = GlobalScope.launch()
		{
			while (this.isActive)
			{
				val result: ChannelResult<Unit> = this@ServerManagerSession.requestChannel.trySend(
					bouncerSessionRequest()
					{
						ping = ping {}
					}
				)

				if (!result.isSuccess)
				{
					break
				}

				delay(Duration.ofSeconds(settings.pingInterval.seconds))
			}
		}
	}

	private fun <T> sendRequestAsync(builder: BouncerSessionRequest.Builder): Deferred<T>
	{
		val requestId: Int = this.nextRequestId.incrementAndGet()
		val deferred: CompletableDeferred<T> = CompletableDeferred()

		this.requestResponses[requestId] = deferred
		this.requestChannel.trySend(builder.setRequestId(requestId).build())

		return deferred
	}

	private fun sendRequestAndForget(request: BouncerSessionRequest)
	{
		this.requestChannel.trySend(request)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	internal fun registerServer(server: BouncerServer)
	{
		val trackingId: Int = this.nextTrackingId.incrementAndGet()

		server.prepare(this, trackingId)
		{ players ->
			val job: Deferred<BouncerSessionResponse.ServerRegistration> = this.sendRequestAsync(
				BouncerSessionRequest.newBuilder()
				.setServerRegistration(
					serverRegistration()
					{
						this.trackingId = trackingId
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
								maxMemory = server.info.maxMemory!!
							}
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

				// Never add it to the list of servers if we have been unregistered
				val serverId: Int = job.getCompleted().serverId
				if (!server.registered(this, serverId))
				{
					// Also send the unregistration, so it gets out of the load balancer queue
					// NOTE: USE TRACKING ID! **NOT** SERVER ID!
					return@invokeOnCompletion this@ServerManagerSession.sendUnregisterServer(trackingId)
				}

				this@ServerManagerSession.serversByServerId[serverId] = server

				if (!server.registered && this@ServerManagerSession.serversByServerId.remove(serverId, server))
				{
					// NOTE: USE TRACKING ID! **NOT** SERVER ID!
					this@ServerManagerSession.sendUnregisterServer(trackingId)
				}
			}
		}
	}

	internal fun unregisterServer(server: BouncerServer, trackingId: Int, serverId: Int)
	{
		if (!this.serversByServerId.remove(serverId, server))
		{
			return
		}

		// NOTE: USE TRACKING ID! **NOT** SERVER ID!
		this.sendUnregisterServer(trackingId)
	}

	private fun sendUnregisterServer(trackingId: Int)
	{
		this.sendRequestAndForget(
			bouncerSessionRequest()
			{
				serverUnregistration = serverUnregistration()
				{
					this.trackingId = trackingId
				}
			}
		)
	}

	internal fun sendUpdate(update: BouncerSessionRequest.ServerUpdate)
	{
		this.sendRequestAndForget(
			bouncerSessionRequest()
			{
				this.serverUpdate = update
			}
		)
	}

	internal fun shutdown(intentional: Boolean = false)
	{
		this.serversByServerId.values.forEach { server -> server.lostConnection() }

		this.requestChannel.trySend(
			bouncerSessionRequest()
			{
				close = close()
				{
					this.intentional = intentional
				}
			}
		)

		this.requestChannel.close()
	}
}
