package io.quut.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.proxy.ProxyServer
import io.quut.bouncer.api.server.IBouncerServerFilter
import io.quut.bouncer.api.server.IBouncerServerWatchRequest
import io.quut.bouncer.api.server.IBouncerServerWatcher
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.velocity.commands.PlayCommand
import io.quut.bouncer.velocity.listeners.PlayerListener
import io.quut.bouncer.velocity.listeners.ServerLoginPluginListener
import io.quut.bouncer.velocity.server.DynamicServerEventHandler
import io.quut.bouncer.velocity.server.VelocityBouncerServerManager
import org.slf4j.Logger

@Plugin(id = "bouncer", name = "Bouncer", version = "1.0", url = "https://quut.io", authors = [ "Joni Aromaa (isokissa3)", "Ossi Erkkilä (avaruus1)" ])
class VelocityBouncerPluginLoader @Inject constructor(private val logger: Logger, private val proxy: ProxyServer)
{
	private val networkManager: NetworkManager = NetworkManager()
	private val userManager: UserManager = UserManager()
	private val serverManager: VelocityBouncerServerManager = VelocityBouncerServerManager(this.networkManager, this.userManager)
	private val bouncer: VelocityBouncerPlugin = VelocityBouncerPlugin(this, this.proxy, this.networkManager, this.serverManager)

	private val dynamicServers: DynamicServerEventHandler = DynamicServerEventHandler(this.logger, this.proxy)

	private lateinit var serverWatcher: IBouncerServerWatcher

	@Subscribe
	fun onProxyInitialize(event: ProxyInitializeEvent)
	{
		this.bouncer.load()
		this.bouncer.enable(this.proxy.boundAddress)

		val loginPluginListener = ServerLoginPluginListener()

		this.proxy.commandManager.register(
			this.proxy.commandManager.metaBuilder("play")
				.plugin(this)
				.build(),
			PlayCommand.createPlayCommand(this.networkManager.stub, this.dynamicServers, loginPluginListener))

		this.proxy.eventManager.register(this, PlayerListener(this.networkManager, this.dynamicServers))
		this.proxy.eventManager.register(this, loginPluginListener)

		this.serverWatcher = this.bouncer.serverManager.watch(IBouncerServerWatchRequest.of(this.dynamicServers, IBouncerServerFilter.IGroup.of("proxy").not()))
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent)
	{
		this.serverWatcher.close()

		this.bouncer.shutdownNow()
	}
}
