package io.quut.bouncer.common.server

import io.quut.bouncer.api.server.IBouncerServerEventHandler
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerWatcher
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.grpc.BouncerWatchRequest
import io.quut.bouncer.grpc.BouncerWatchResponse
import io.quut.bouncer.grpc.ServerData
import io.quut.bouncer.grpc.ServerRemovelReason
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class ServerWatcher(private val networkManager: NetworkManager, private val eventHandler: IBouncerServerEventHandler) : IBouncerServerWatcher
{
	private val servers: ConcurrentMap<Int, IBouncerServerInfo> = ConcurrentHashMap()

	private lateinit var job: Job

	@OptIn(DelicateCoroutinesApi::class)
	internal fun start(request: BouncerWatchRequest)
	{
		this.job = GlobalScope.launch()
		{
			while (this.isActive)
			{
				runCatching { this@ServerWatcher.startInternal(request) }

				delay(3333)
			}
		}
	}

	private suspend fun startInternal(request: BouncerWatchRequest)
	{
		this.servers.forEach { (id, server) -> this.eventHandler.removeServer(id, server, IBouncerServerEventHandler.RemoveReason.ERROR) }
		this.servers.clear()

		this.networkManager.stub.watch(request).cancellable().collect(this::handleResponse)
	}

	private fun handleResponse(response: BouncerWatchResponse)
	{
		when (response.dataCase)
		{
			BouncerWatchResponse.DataCase.SERVER -> this.handleServerResponse(response.server)

			else -> Unit
		}
	}

	private fun handleServerResponse(response: BouncerWatchResponse.Server)
	{
		when (response.updateCase)
		{
			BouncerWatchResponse.Server.UpdateCase.ADD ->
			{
				val serverData: ServerData = response.add.data
				val address: InetSocketAddress = InetSocketAddress.createUnresolved(serverData.host, serverData.port)
				val server: IBouncerServerInfo = IBouncerServerInfo.of(serverData.name, serverData.group, serverData.type, address)

				this.servers[response.serverId] = server

				this.eventHandler.addServer(response.serverId, server)
			}
			BouncerWatchResponse.Server.UpdateCase.REMOVE ->
			{
				val server: IBouncerServerInfo = this.servers.remove(response.serverId) ?: return

				val reason: IBouncerServerEventHandler.RemoveReason = when (response.remove.reason)
				{
					ServerRemovelReason.UNREGISTRATION -> IBouncerServerEventHandler.RemoveReason.UNREGISTER
					ServerRemovelReason.TIMEOUT -> IBouncerServerEventHandler.RemoveReason.TIMEOUT
					else -> IBouncerServerEventHandler.RemoveReason.UNSPECIFIED
				}

				this.eventHandler.removeServer(response.serverId, server, reason)
			}
			else -> Unit
		}
	}

	override fun close()
	{
		this.job.cancel()
	}
}
