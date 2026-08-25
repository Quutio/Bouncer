package io.quut.bouncer.velocity

import com.google.inject.Inject
import com.velocitypowered.api.command.CommandManager
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent
import com.velocitypowered.api.plugin.PluginContainer
import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.api.IBouncerAPI
import io.quut.bouncer.api.server.IDistributedServerFilter
import io.quut.bouncer.api.server.IDistributedServerWatchRequest
import io.quut.bouncer.api.server.IDistributedServerWatcher
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.velocity.commands.PlayCommand
import io.quut.bouncer.velocity.listeners.PlayerListener
import io.quut.bouncer.velocity.listeners.PluginMessageListener
import io.quut.bouncer.velocity.server.DynamicServerEventHandler
import io.quut.bouncer.velocity.server.VelocityDistributedServerManager
import io.quut.bouncer.velocity.universe.VelocityDistributedUniverseManager

internal class VelocityBouncerPluginBootstrap @Inject constructor(
	private val container: PluginContainer,
	private val plugin: VelocityBouncerPlugin,
	private val defaultServer: VelocityBouncerDefaultServer,
	private val networkManager: NetworkManager,
	private val serverManager: VelocityDistributedServerManager,
	private val universeManager: VelocityDistributedUniverseManager,
	private val dynamicServers: DynamicServerEventHandler,
	private val commandManager: CommandManager,
	private val eventManager: EventManager)
{
	private val api: API = API()

	private lateinit var serverWatcher: IDistributedServerWatcher

	@Subscribe
	fun onProxyInitialize(event: ProxyInitializeEvent)
	{
		this.plugin.load()
		this.defaultServer.load()

		this.commandManager.register(this.commandManager.metaBuilder("play").plugin(this.container).build(),
			PlayCommand.createPlayCommand(this.universeManager))

		this.eventManager.register(this.container, PlayerListener(this.networkManager, this.dynamicServers))
		this.eventManager.register(this.container, PluginMessageListener(this.universeManager))

		this.serverWatcher = this.serverManager.watch(IDistributedServerWatchRequest.of(this.dynamicServers, IDistributedServerFilter.IGroup.of("proxy").not()))

		IBouncerAPI.register(this.api)
	}

	@Subscribe
	fun onProxyShutdown(event: ProxyShutdownEvent)
	{
		this.serverWatcher.close()

		this.plugin.shutdownNow()

		IBouncerAPI.unregister(this.api)
	}

	private inner class API : IBouncerAPI
	{
		override val bouncer: IBouncer
			get() = this@VelocityBouncerPluginBootstrap.plugin
	}
}
