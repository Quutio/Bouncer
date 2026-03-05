package io.quut.bouncer.velocity.server

import com.google.inject.Inject
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.quut.bouncer.api.server.IDistributedServerEventHandler
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.api.node.IDistributedNodeState
import io.quut.bouncer.api.universe.IDistributedUniverseInfo
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo
import org.slf4j.Logger
import java.net.InetSocketAddress

internal class DynamicServerEventHandler @Inject constructor(private val logger: Logger, private val proxy: ProxyServer) : IDistributedServerEventHandler
{
	private val servers: MutableMap<Int, RegisteredServer> = hashMapOf()
	private val universes: MutableMap<Int, Triple<IDistributedUniverseInfo, IDistributedUniverseSupervisorInstanceInfo, IDistributedNodeState>> = hashMapOf()

	internal fun getServer(id: Int): RegisteredServer? = this.servers[id]
	internal fun getUniverse(id: Int): Triple<IDistributedUniverseInfo, IDistributedUniverseSupervisorInstanceInfo, IDistributedNodeState>? = this.universes[id]

	override fun addServer(id: Int, info: IDistributedServerInfo, state: IDistributedServerState)
	{
		val address: InetSocketAddress = state.address ?: return

		this.registerServer(id, info, address)
	}

	override fun updateServer(id: Int, info: IDistributedServerInfo, state: IDistributedServerState)
	{
		val address: InetSocketAddress = state.address ?: return this.unregisterServer(id, info, IDistributedServerEventHandler.RemoveReason.UNREGISTER)
		val registeredServer: RegisteredServer = this.servers[id] ?: return this.registerServer(id, info, address)

		if (registeredServer.serverInfo.address != address)
		{
			this.unregisterServer(id, info, IDistributedServerEventHandler.RemoveReason.UNREGISTER)
			this.registerServer(id, info, address)
		}
	}

	override fun removeServer(id: Int, info: IDistributedServerInfo, state: IDistributedServerState, reason: IDistributedServerEventHandler.RemoveReason) =
		this.unregisterServer(id, info, reason)

	override fun addUniverse(id: Int, info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedNodeState)
	{
		this.universes[id] = Triple(info, supervisor, state)
	}

	override fun updateUniverse(id: Int, info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedNodeState)
	{
		this.universes.computeIfPresent(id) { _, (info, supervisor) -> Triple(info, supervisor, state) }
	}

	override fun removeUniverse(id: Int, info: IDistributedUniverseInfo, supervisor: IDistributedUniverseSupervisorInstanceInfo, state: IDistributedNodeState)
	{
		this.universes.remove(id)
	}

	private fun registerServer(id: Int, info: IDistributedServerInfo, address: InetSocketAddress)
	{
		this.proxy.getServer(info.name).ifPresentOrElse(
			{ registeredServer ->
				val registeredServerAddress: InetSocketAddress = registeredServer.serverInfo.address
				if (registeredServerAddress.hostString == address.hostString && registeredServerAddress.port == address.port)
				{
					this.logger.warn("Unable to register dynamic server {}, it is already present!", info.name)
				}
				else
				{
					this.logger.error("Unable to register dynamic server {} ({}:{}), it is already registered to {}:{}!",
						info.name, address.hostString, address.port, registeredServerAddress.hostString, registeredServerAddress.port)
				}
			})
		{
			this.logger.info("Registering dynamic server {} ({}:{})", info.name, address.hostString, address.port)

			val registeredServer: RegisteredServer = this.proxy.registerServer(ServerInfo(info.name, address))

			this.servers[id] = registeredServer
		}
	}

	private fun unregisterServer(id: Int, info: IDistributedServerInfo, reason: IDistributedServerEventHandler.RemoveReason)
	{
		val registeredServer: RegisteredServer = this.servers.remove(id) ?: return

		if (reason == IDistributedServerEventHandler.RemoveReason.UNREGISTER)
		{
			this.logger.info("Unregistering dynamic server {}", info.name)
		}
		else
		{
			this.logger.warn("Removing dynamic server {} due to {}", info.name, reason)
		}

		this.proxy.unregisterServer(registeredServer.serverInfo)
	}
}
