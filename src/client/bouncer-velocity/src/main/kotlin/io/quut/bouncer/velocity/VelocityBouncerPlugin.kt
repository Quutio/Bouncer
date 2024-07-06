package io.quut.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.BouncerListenRequestKt.server
import io.quut.bouncer.grpc.BouncerListenResponse
import io.quut.bouncer.grpc.ServerData
import io.quut.bouncer.grpc.bouncerListenRequest
import io.quut.bouncer.velocity.commands.PlayCommand
import io.quut.bouncer.velocity.commands.QueueCommand
import io.quut.bouncer.velocity.listeners.PlayerListener
import io.quut.bouncer.velocity.queue.QueueManager
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.net.InetSocketAddress

@Plugin(id = "bouncer", name = "Bouncer", version = "1.0", url = "https://quut.io", authors = [ "Joni Aromaa (isokissa3)", "Ossi Erkkilä (avaruus1)" ])
class VelocityBouncerPlugin @Inject constructor(val proxy: ProxyServer)
{
	private lateinit var bouncer: VelocityBouncerAPI

	internal val serversByName: MutableMap<String, IBouncerServerInfo> = mutableMapOf()
	internal val serversById: MutableMap<Int, Pair<IBouncerServerInfo, RegisteredServer>> = mutableMapOf()

	private val bouncerAddress: String = System.getenv("BOUNCER_ADDRESS") ?: "localhost:5000"

	internal val channel: ManagedChannel = ManagedChannelBuilder.forTarget(bouncerAddress).usePlaintext().build()
	internal val stub: BouncerGrpcKt.BouncerCoroutineStub = BouncerGrpcKt.BouncerCoroutineStub(channel)

	@Subscribe
	fun onProxyInitialize(event: ProxyInitializeEvent)
	{
		this.bouncer = VelocityBouncerAPI(this, this.bouncerAddress)
		this.bouncer.installShutdownSignal()

		this.proxy.eventManager.register(this, PlayerListener(this))

		this.proxy.commandManager.register(
			this.proxy.commandManager.metaBuilder("queue")
			.plugin(this)
			.build(),
				QueueCommand.createQueueCommand(QueueManager(this.stub, this.proxy))
		)

		this.proxy.commandManager.register(
			this.proxy.commandManager.metaBuilder("play")
			.plugin(this)
			.build(),
				PlayCommand.createPlayCommand(this, this.stub)
		)

		@OptIn(DelicateCoroutinesApi::class)
		GlobalScope.launch()
		{
			suspend fun startListening()
			{
				this@VelocityBouncerPlugin.stub.listen(
					bouncerListenRequest()
					{
						this.server = server {}
					}
				).collect(this@VelocityBouncerPlugin::handleResponse)
			}

			while (true)
			{
				runCatching { startListening() }

				delay(3333)
			}
		}
	}

	private fun handleResponse(response: BouncerListenResponse)
	{
		when (response.dataCase)
		{
			BouncerListenResponse.DataCase.SERVER -> this.handleServerResponse(response.server)

			else -> Unit
		}
	}

	private fun handleServerResponse(response: BouncerListenResponse.Server)
	{
		when (response.updateCase)
		{
			BouncerListenResponse.Server.UpdateCase.ADD ->
			{
				val serverData: ServerData = response.add.data
				val address: InetSocketAddress = InetSocketAddress.createUnresolved(serverData.host, serverData.port)
				val registeredServer: RegisteredServer = this@VelocityBouncerPlugin.proxy.registerServer(ServerInfo(serverData.name, address))

				println("Add server ${response.serverId} (${serverData.host}:${serverData.port})")
				val info = IBouncerServerInfo.of(serverData.name, serverData.group, serverData.type, address)

				this@VelocityBouncerPlugin.serversById[response.serverId] = Pair(info, registeredServer)
				this@VelocityBouncerPlugin.serversByName[serverData.name] = info
			}

			BouncerListenResponse.Server.UpdateCase.REMOVE ->
			{
				println("Remove server ${response.serverId}")
				val server: IBouncerServerInfo =
					this@VelocityBouncerPlugin.serversById.remove(response.serverId)?.first ?: return

				this@VelocityBouncerPlugin.serversByName.remove(server.name)
				this@VelocityBouncerPlugin.proxy.unregisterServer(ServerInfo(server.name, server.address))
			}

			else -> Unit
		}
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent)
	{
	}
}
