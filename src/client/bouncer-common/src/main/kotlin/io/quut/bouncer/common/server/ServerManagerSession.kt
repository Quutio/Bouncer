package io.quut.bouncer.common.server

import io.quut.bouncer.grpc.ServerServiceGrpcKt
import io.quut.bouncer.grpc.ServerSessionRequest
import io.quut.bouncer.grpc.ServerSessionResponse
import io.quut.bouncer.grpc.ServerStatusUpdate
import io.quut.bouncer.grpc.serverData
import io.quut.bouncer.grpc.serverRegistrationRequest
import io.quut.bouncer.grpc.serverUnregistrationRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

internal class ServerManagerSession(private val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub)
{
	private val nextRequestId: AtomicInteger = AtomicInteger(1)
	private val requestChannel: Channel<ServerSessionRequest> = Channel(capacity = Channel.UNLIMITED)
	private val requestResponses: ConcurrentMap<Int, CompletableDeferred<ServerSessionResponse>> = ConcurrentHashMap()

	private val servers: ConcurrentMap<Int, BouncerServer> = ConcurrentHashMap()

	internal suspend fun start()
	{
		coroutineScope()
		{
			this@ServerManagerSession.stub.session(this@ServerManagerSession.requestChannel.receiveAsFlow())
				.cancellable()
				.collect()
				{ response ->
					this@ServerManagerSession.requestResponses.remove(response.requestId)?.complete(response)

					if (this@ServerManagerSession.servers.isEmpty())
					{
						this.cancel()
					}
				}
		}
	}

	private fun sendRequestAsync(builder: ServerSessionRequest.Builder): Deferred<ServerSessionResponse>
	{
		val requestId: Int = this.nextRequestId.getAndIncrement()
		val deferred: CompletableDeferred<ServerSessionResponse> = CompletableDeferred()

		this.requestResponses[requestId] = deferred
		this.requestChannel.trySend(builder.setRequestId(requestId).build())

		return deferred
	}

	private fun sendRequestAndForget(builder: ServerSessionRequest.Builder)
	{
		this.requestChannel.trySend(
			builder
				.setRequestId(this.nextRequestId.getAndIncrement())
				.build()
		)
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	internal fun registerServer(server: BouncerServer)
	{
		val job: Deferred<ServerSessionResponse> = this.sendRequestAsync(
			ServerSessionRequest.newBuilder().setRegistration(
				serverRegistrationRequest()
				{
					this.data = serverData()
					{
						this.name = server.info.name
						this.group = server.info.group
						this.type = server.info.type
						this.host = server.info.address.hostString
						this.port = server.info.address.port

						if (server.info.maxMemory != null)
						{
							this.maxMemory = server.info.maxMemory!!
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

			val serverId: Int = job.getCompleted().registration.serverId // Never add it to the list of servers if we have been unregistered
			if (!server.registered(this, serverId))
			{
				// Also send the unregistration, so it gets out of the load balancer queue
				return@invokeOnCompletion this@ServerManagerSession.sendUnregisterServer(serverId)
			}

			this@ServerManagerSession.servers[server.id] = server

			if (!server.registered && this@ServerManagerSession.servers.remove(server.id, server))
			{
				this@ServerManagerSession.sendUnregisterServer(serverId)
			}
		}
	}

	private fun sendUnregisterServer(id: Int)
	{
		this.sendRequestAndForget(
			ServerSessionRequest.newBuilder().setUnregistration(
				serverUnregistrationRequest()
				{
					this.serverId = id
				}
			)
		)
	}

	internal fun sendUpdate(update: ServerStatusUpdate)
	{
		this.sendRequestAndForget(
			ServerSessionRequest
				.newBuilder()
				.setUpdate(update)
		)
	}

	internal fun unregisterServer(server: BouncerServer)
	{
		// If we never succeeded registering the server we are done
		if (!server.unregister())
		{
			return
		}

		if (!this.servers.remove(server.id, server))
		{
			return
		}

		this.sendUnregisterServer(server.id)
	}

	internal fun shutdown()
	{
		this.servers.values.forEach { server -> this.unregisterServer(server) }

		this.requestChannel.close()
	}
}
