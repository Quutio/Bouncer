package fi.joniaromaa.bouncer.common.server

import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.api.server.IBouncerServer
import fi.joniaromaa.bouncer.api.server.IServerLoadBalancer
import fi.joniaromaa.bouncer.grpc.ServerData
import fi.joniaromaa.bouncer.grpc.ServerRegistrationRequest
import fi.joniaromaa.bouncer.grpc.ServerServiceGrpcKt
import fi.joniaromaa.bouncer.grpc.ServerSessionRequest
import fi.joniaromaa.bouncer.grpc.ServerSessionResponse
import fi.joniaromaa.bouncer.grpc.ServerStatusUpdate
import fi.joniaromaa.bouncer.grpc.ServerUnregistrationRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

internal class ServerLoadBalancer(private val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub) : IServerLoadBalancer
{
	private val servers: ConcurrentMap<Int, BouncerServer> = ConcurrentHashMap()
	private val nextRequestId: AtomicInteger = AtomicInteger(1)
	private val requestResponse: ConcurrentMap<Int, CompletableDeferred<ServerSessionResponse>> = ConcurrentHashMap()
	private val requestChannel: Channel<ServerSessionRequest> = Channel(capacity = Channel.Factory.UNLIMITED)

	init
	{
		GlobalScope.launch {
			while (true)
			{
				try
				{
					this@ServerLoadBalancer.createSession()
				}
				catch (e: Throwable)
				{
					e.printStackTrace()
				}

				delay(TimeUnit.SECONDS.toMillis(3)) //Wait for 3s before reconnecting
			}
		}
	}

	private suspend fun createSession()
	{
		try
		{
			this.stub.session(this.requestChannel.receiveAsFlow()).collect { response ->
				this.requestResponse.remove(response.requestId)?.complete(response)
			}
		}
		finally
		{
			for (server: BouncerServer in this.servers.values)
			{
				server.lostConnection()	//Register the server async

				GlobalScope.launch {
					this@ServerLoadBalancer.registerServer(server)
				}
			}

			this.servers.clear()
		}
	}

	private fun sendRequestAsync(builder: ServerSessionRequest.Builder): Deferred<ServerSessionResponse>
	{
		val requestId: Int = this.nextRequestId.getAndIncrement()
		val deferred: CompletableDeferred<ServerSessionResponse> = CompletableDeferred()

		this.requestResponse[requestId] = deferred
		this.requestChannel.trySend(builder.setRequestId(requestId).build()).isSuccess

		return deferred
	}

	private fun sendRequestForgetAsync(builder: ServerSessionRequest.Builder)
	{
		this.requestChannel.trySend(builder.build()).isSuccess
	}

	override fun registerServer(info: BouncerServerInfo): IBouncerServer
	{
		val server = BouncerServer(this, info) //Register the server async
		
		GlobalScope.launch {
			this@ServerLoadBalancer.registerServer(server)
		}

		//Return the server instance already so it can be mutated
		return server
	}

	private suspend fun registerServer(server: BouncerServer)
	{
		val response: ServerSessionResponse = this@ServerLoadBalancer.sendRequestAsync(ServerSessionRequest.newBuilder()
			.setRegistration(ServerRegistrationRequest.newBuilder()
				.setData(ServerData.newBuilder()
					.setName(server.info.name)
					.setGroup(server.info.group)
					.setType(server.info.type)
					.setHost(server.info.address.hostString)
					.setPort(server.info.address.port)
				)
			)
		).await()

		val serverId: Int = response.registration.serverId //Never add it to the list of servers if we have been unregistered
		if (!server.registered(serverId))
		{
			//Also send the unregistration so it gets out of the load balancer queue
			return this@ServerLoadBalancer.sendUnregisterServer(serverId)
		}

		this@ServerLoadBalancer.servers[server.id] = server
	}

	override fun unregisterServer(server: IBouncerServer)
	{
		this.unregisterServer(server as BouncerServer)
	}

	private fun unregisterServer(server: BouncerServer)
	{
		//Check whatever we have already been unregistered or we never were fully unregister
		if (!server.unregister())
		{
			return
		}

		this.servers.remove(server.id)

		this.sendUnregisterServer(server.id)
	}

	private fun sendUnregisterServer(serverId: Int)
	{
		this@ServerLoadBalancer.sendRequestForgetAsync(ServerSessionRequest.newBuilder()
			.setUnregistration(ServerUnregistrationRequest.newBuilder()
				.setServerId(serverId)
			)
		)
	}

	internal fun sendUpdateAsync(update: ServerStatusUpdate)
	{
		this.sendRequestForgetAsync(ServerSessionRequest.newBuilder().setUpdate(update))
	}

	internal fun shutdown()
	{
		for(server: BouncerServer in this.servers.values)
		{
			this.unregisterServer(server)
		}
	}
}