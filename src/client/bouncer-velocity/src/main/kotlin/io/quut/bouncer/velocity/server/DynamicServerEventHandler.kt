package io.quut.bouncer.velocity.server

import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.quut.bouncer.api.server.IBouncerServerEventHandler
import io.quut.bouncer.api.server.IBouncerServerInfo
import org.slf4j.Logger

internal class DynamicServerEventHandler(private val logger: Logger, private val proxy: ProxyServer) : IBouncerServerEventHandler
{
	private val servers: MutableMap<Int, RegisteredServer> = hashMapOf()

	internal operator fun get(id: Int): RegisteredServer? = this.servers[id]

	override fun addServer(id: Int, server: IBouncerServerInfo)
	{
		this.logger.info("Registering dynamic server {} ({}:{})", server.name, server.address.hostString, server.address.port)

		val registeredServer: RegisteredServer = this.proxy.registerServer(ServerInfo(server.name, server.address))

		this.servers[id] = registeredServer
	}

	override fun removeServer(id: Int, server: IBouncerServerInfo, reason: IBouncerServerEventHandler.RemoveReason)
	{
		val registeredServer: RegisteredServer = this.servers.remove(id) ?: return

		if (reason == IBouncerServerEventHandler.RemoveReason.UNREGISTER)
		{
			this.logger.info("Unregistering dynamic server {}", server.name)
		}
		else
		{
			this.logger.warn("Removing dynamic server {} due to {}", server.name, reason)
		}

		this.proxy.unregisterServer(registeredServer.serverInfo)
	}
}
