package io.quut.bouncer.common.server

import io.quut.bouncer.api.server.IDistributedServerEventHandler
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.api.server.IDistributedServerWatcher
import io.quut.bouncer.api.unit.IDistributedUnitState
import io.quut.bouncer.api.universe.IDistributedUniverseInfo
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.grpc.BouncerWatchRequest
import io.quut.bouncer.grpc.BouncerWatchResponse
import io.quut.bouncer.grpc.ServerData
import io.quut.bouncer.grpc.ServerRemovelReason
import io.quut.bouncer.grpc.ServerState
import io.quut.bouncer.grpc.State
import io.quut.bouncer.grpc.UniverseData
import io.quut.bouncer.grpc.addressOrNull
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import java.net.InetSocketAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class ServerWatcher(private val networkManager: NetworkManager, private val eventHandler: IDistributedServerEventHandler) : IDistributedServerWatcher
{
	private val servers: ConcurrentMap<Int, Pair<IDistributedServerInfo, IDistributedServerState>> = ConcurrentHashMap()
	private val universes: ConcurrentMap<Int, Triple<IDistributedUniverseInfo, IDistributedUniverseSupervisorInstanceInfo, IDistributedUnitState>> = ConcurrentHashMap()

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
		this.servers.forEach { id, (info, state) -> this.eventHandler.removeServer(id, info, state, IDistributedServerEventHandler.RemoveReason.ERROR) }
		this.servers.clear()

		this.universes.forEach { id, (info, supervisor, state) -> this.eventHandler.removeUniverse(id, info, supervisor, state) }
		this.universes.clear()

		this.networkManager.stub.watch(request).cancellable().collect(this::handleResponse)
	}

	private fun handleResponse(response: BouncerWatchResponse)
	{
		when (response.dataCase)
		{
			BouncerWatchResponse.DataCase.SERVER -> this.handleServerResponse(response.server)
			BouncerWatchResponse.DataCase.UNIVERSE -> this.handleUniverseResponse(response.universe)

			else -> Unit
		}
	}

	private fun handleServerResponse(response: BouncerWatchResponse.Server)
	{
		when (response.actionCase)
		{
			BouncerWatchResponse.Server.ActionCase.ADD ->
			{
				val info: IDistributedServerInfo = this.info(response.add.data)
				val state: IDistributedServerState = this.state(response.add.state)

				this.servers[response.serverId] = Pair(info, state)

				this.eventHandler.addServer(response.serverId, info, state)
			}
			BouncerWatchResponse.Server.ActionCase.UPDATE ->
			{
				val state: IDistributedServerState = this.state(response.update.state)

				this.servers.computeIfPresent(response.serverId) { _, (info) -> Pair(info, state) }?.let()
					{ (info, state) -> this.eventHandler.updateServer(response.serverId, info, state) }
			}
			BouncerWatchResponse.Server.ActionCase.REMOVE ->
			{
				val (info: IDistributedServerInfo, state: IDistributedServerState) = this.servers.remove(response.serverId) ?: return

				val reason: IDistributedServerEventHandler.RemoveReason = when (response.remove.reason)
				{
					ServerRemovelReason.UNREGISTRATION -> IDistributedServerEventHandler.RemoveReason.UNREGISTER
					ServerRemovelReason.TIMEOUT -> IDistributedServerEventHandler.RemoveReason.TIMEOUT
					else -> IDistributedServerEventHandler.RemoveReason.UNSPECIFIED
				}

				this.eventHandler.removeServer(response.serverId, info, state, reason)
			}
			else -> Unit
		}
	}

	private fun handleUniverseResponse(response: BouncerWatchResponse.Universe)
	{
		when (response.actionCase)
		{
			BouncerWatchResponse.Universe.ActionCase.ADD ->
			{
				val (info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo) = this.info(response.add.data)
				val state: IDistributedUnitState = this.state(response.add.state)

				this.universes[response.universeId] = Triple(info, supervisor, state)

				this.eventHandler.addUniverse(response.universeId, info, supervisor, state)
			}
			BouncerWatchResponse.Universe.ActionCase.UPDATE ->
			{
				val state: IDistributedUnitState = this.state(response.update.state)

				this.universes.computeIfPresent(response.serverId) { _, (info, supervisor) -> Triple(info, supervisor, state) }?.let()
				{ (info, supervisor, state) -> this.eventHandler.updateUniverse(response.serverId, info, supervisor, state) }
			}
			BouncerWatchResponse.Universe.ActionCase.REMOVE ->
			{
				val (info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedUnitState) = this.universes.remove(response.universeId) ?: return

				this.eventHandler.removeUniverse(response.serverId, info, supervisor, state)
			}
			else -> Unit
		}
	}

	private fun info(serverData: ServerData): IDistributedServerInfo =
		IDistributedServerInfo.of(serverData.name, serverData.group, serverData.type)

	private fun info(universeData: UniverseData): Pair<IDistributedUniverseInfo, IDistributedUniverseSupervisorInstanceInfo> =
		Pair(IDistributedUniverseInfo.of(Key.key(universeData.type)), IDistributedUniverseSupervisorInstanceInfo.of(Key.key(universeData.supervisor.type), universeData.supervisor.attributesMap))

	private fun state(state: ServerState): IDistributedServerState
	{
		val address: InetSocketAddress? = state.addressOrNull?.let { a -> InetSocketAddress.createUnresolved(a.host, a.port) }

		return IDistributedServerState.of(Key.key(state.state.type), address, state.state.maxPlayers)
	}

	private fun state(state: State): IDistributedUnitState = IDistributedUnitState.of(Key.key(state.type), state.maxPlayers)

	override fun close()
	{
		this.job.cancel()
	}
}
