package fi.joniaromaa.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.ServerInfo
import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.grpc.ServerData
import fi.joniaromaa.bouncer.grpc.ServerListenRequest
import fi.joniaromaa.bouncer.grpc.ServerServiceGrpcKt
import fi.joniaromaa.bouncer.grpc.ServerStatusUpdate
import fi.joniaromaa.bouncer.velocity.listeners.PlayerListener
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

@Plugin(id = "bouncer", name = "Bouncer", version = "1.0", url = "https://joniaromaa.fi", authors = [ "Joni Aromaa (isokissa3)", "Ossi Erkkilä (avaruus1)" ])
class VelocityBouncerPlugin @Inject constructor(val proxy: ProxyServer)
{
	private lateinit var bouncer: VelocityBouncerAPI

	internal val serversByName: MutableMap<String, BouncerServerInfo> = mutableMapOf()
	internal val serversById: MutableMap<Int, BouncerServerInfo> = mutableMapOf()

	private val bouncerAddress: String = System.getenv("BOUNCER_ADDRESS") ?: "localhost:5000"

	internal val channel: ManagedChannel = ManagedChannelBuilder.forTarget(bouncerAddress).usePlaintext().build()
	internal val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub = ServerServiceGrpcKt.ServerServiceCoroutineStub(channel)

	@Subscribe
	fun onProxyInitialize(event: ProxyInitializeEvent)
	{
		this.proxy.eventManager.register(this, PlayerListener(this))

		bouncer = VelocityBouncerAPI(this, bouncerAddress)

		suspend fun startListening()
		{
			stub.listen(ServerListenRequest.newBuilder().build()).collect()
			{ update ->
				when (update.updateCase)
				{
					ServerStatusUpdate.UpdateCase.ADD ->
					{
						val serverData: ServerData = update.add.data
						val address: InetSocketAddress = InetSocketAddress.createUnresolved(serverData.host, serverData.port)
						this@VelocityBouncerPlugin.proxy.registerServer(ServerInfo(serverData.name, address))

						println("Add server ${update.serverId} (${serverData.host}:${serverData.port})")

						val info = BouncerServerInfo(serverData.name, serverData.group, serverData.type, address)

						this@VelocityBouncerPlugin.serversById[update.serverId] = info
						this@VelocityBouncerPlugin.serversByName[serverData.name] = info
					}
					ServerStatusUpdate.UpdateCase.REMOVE ->
					{
						println("Remove server ${update.serverId}")
						val server: BouncerServerInfo = this@VelocityBouncerPlugin.serversById.remove(update.serverId) ?: return@collect

						this@VelocityBouncerPlugin.serversByName.remove(server.name)
						this@VelocityBouncerPlugin.proxy.unregisterServer(ServerInfo(server.name, server.address))
					}
					else -> Unit
				}
			}
		}

		GlobalScope.launch()
		{
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
