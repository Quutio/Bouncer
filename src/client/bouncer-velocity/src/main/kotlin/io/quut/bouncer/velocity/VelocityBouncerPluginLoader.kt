package io.quut.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import com.velocitypowered.api.proxy.server.RegisteredServer
import com.velocitypowered.api.proxy.server.ServerInfo
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.grpc.BouncerListenRequestKt.server
import io.quut.bouncer.grpc.BouncerListenResponse
import io.quut.bouncer.grpc.ServerData
import io.quut.bouncer.grpc.bouncerListenRequest
import io.quut.bouncer.velocity.commands.PlayCommand
import io.quut.bouncer.velocity.commands.QueueCommand
import io.quut.bouncer.velocity.listeners.PlayerListener
import io.quut.bouncer.velocity.queue.QueueManager
import io.quut.bouncer.velocity.server.VelocityServerManager
import java.net.InetSocketAddress
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Plugin(id = "bouncer", name = "Bouncer", version = "1.0", url = "https://quut.io", authors = [ "Joni Aromaa (isokissa3)", "Ossi Erkkilä (avaruus1)" ])
class VelocityBouncerPluginLoader @Inject constructor(val proxy: ProxyServer)
{
	private val networkManager: NetworkManager = NetworkManager()
	private val userManager: UserManager = UserManager()
	private val serverManager: VelocityServerManager = VelocityServerManager(this.networkManager, this.userManager)
	private val bouncer: VelocityBouncerPlugin = VelocityBouncerPlugin(this, this.proxy, this.serverManager)

	internal val serversByName: MutableMap<String, IBouncerServerInfo> = mutableMapOf()
	internal val serversById: MutableMap<Int, Pair<IBouncerServerInfo, RegisteredServer>> = mutableMapOf()

	@Subscribe
	fun onProxyInitialize(event: ProxyInitializeEvent)
	{
		this.bouncer.load()
		this.bouncer.enable(this.proxy.boundAddress)

		this.proxy.eventManager.register(this, PlayerListener(this, this.networkManager))

		this.proxy.commandManager.register(
			this.proxy.commandManager.metaBuilder("queue")
				.plugin(this)
				.build(),
			QueueCommand.createQueueCommand(QueueManager(this.networkManager.stub, this.proxy)))

		this.proxy.commandManager.register(
			this.proxy.commandManager.metaBuilder("play")
				.plugin(this)
				.build(),
			PlayCommand.createPlayCommand(this, this.networkManager.stub))

		@OptIn(DelicateCoroutinesApi::class)
		GlobalScope.launch()
		{
			suspend fun startListening()
			{
				this@VelocityBouncerPluginLoader.networkManager.stub.listen(
					bouncerListenRequest()
					{
						this.server = server {}
					}
				).collect(this@VelocityBouncerPluginLoader::handleResponse)
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
				val registeredServer: RegisteredServer = this@VelocityBouncerPluginLoader.proxy.registerServer(ServerInfo(serverData.name, address))

				println("Add server ${response.serverId} (${serverData.host}:${serverData.port})")
				val info = IBouncerServerInfo.of(serverData.name, serverData.group, serverData.type, address)

				this@VelocityBouncerPluginLoader.serversById[response.serverId] = Pair(info, registeredServer)
				this@VelocityBouncerPluginLoader.serversByName[serverData.name] = info
			}

			BouncerListenResponse.Server.UpdateCase.REMOVE ->
			{
				println("Remove server ${response.serverId}")
				val server: IBouncerServerInfo =
					this@VelocityBouncerPluginLoader.serversById.remove(response.serverId)?.first ?: return

				this@VelocityBouncerPluginLoader.serversByName.remove(server.name)
				this@VelocityBouncerPluginLoader.proxy.unregisterServer(ServerInfo(server.name, server.address))
			}

			else -> Unit
		}
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent)
	{
	}
}
