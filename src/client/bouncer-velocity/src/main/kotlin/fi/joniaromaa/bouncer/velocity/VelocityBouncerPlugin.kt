package fi.joniaromaa.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import fi.joniaromaa.bouncer.grpc.ServerData
import fi.joniaromaa.bouncer.grpc.ServerListenRequest
import fi.joniaromaa.bouncer.grpc.ServerServiceGrpcKt
import fi.joniaromaa.bouncer.grpc.ServerStatusUpdate
import fi.joniaromaa.bouncer.velocity.listeners.PlayerListener
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

@Plugin(id = "bouncer", name = "Bouncer", version = "1.0", url = "https://joniaromaa.fi", authors = [ "Joni Aromaa (isokissa3)" ])
class VelocityBouncerPlugin @Inject constructor(private val proxy: ProxyServer)
{
	internal val serversByName: MutableMap<String, RegisteredServer> = mutableMapOf()
	internal val serversById: MutableMap<Int, RegisteredServer> = mutableMapOf()

	internal val channel: ManagedChannel = ManagedChannelBuilder.forTarget(System.getenv("BOUNCER_ADDRESS") ?: "localhost:5000").usePlaintext().build()
	internal val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub = ServerServiceGrpcKt.ServerServiceCoroutineStub(channel)

	@Subscribe
	fun onProxyInitialize(event: ProxyInitializeEvent)
	{
		this.proxy.eventManager.register(this, PlayerListener(this))

		suspend fun startListening() {
			stub.listen(ServerListenRequest.newBuilder().build()).collect { update ->
				when (update.updateCase)
				{
					ServerStatusUpdate.UpdateCase.ADD ->
					{
						val serverData: ServerData = update.add.data
						val address: InetSocketAddress = InetSocketAddress.createUnresolved(serverData.host, serverData.port)
						val server: RegisteredServer = this@VelocityBouncerPlugin.proxy.registerServer(ServerInfo(serverData.name, address))

						println("Add server ${update.serverId} (${serverData.host}:${serverData.port})")

						this@VelocityBouncerPlugin.serversById[update.serverId] = server
						this@VelocityBouncerPlugin.serversByName[serverData.name] = server
					}
					ServerStatusUpdate.UpdateCase.REMOVE ->
					{
						println("Remove server ${update.serverId}")
						val server: RegisteredServer = this@VelocityBouncerPlugin.serversById.remove(update.serverId) ?: return@collect

						this@VelocityBouncerPlugin.serversByName.remove(server.serverInfo.name)
						this@VelocityBouncerPlugin.proxy.unregisterServer(server.serverInfo)
					}
				}
			}
		}

		GlobalScope.launch {
			while (true)
			{
				try
				{
					startListening()
				}
				catch (e: Throwable)
				{
				}

				delay(3333)
			}
		}
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent)
	{

	}
}