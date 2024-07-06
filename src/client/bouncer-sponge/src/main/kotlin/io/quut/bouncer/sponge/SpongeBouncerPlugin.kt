package io.quut.bouncer.sponge

import com.google.inject.Inject
import io.quut.bouncer.api.IBouncerScopeListener
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.BouncerAPI
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.sponge.config.PluginConfig
import io.quut.bouncer.sponge.listeners.CommandListener
import io.quut.bouncer.sponge.listeners.ConnectionListener
import io.quut.bouncer.sponge.listeners.FallbackServerListener
import org.spongepowered.api.Game
import org.spongepowered.api.Server
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.EventManager
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StoppedGameEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.api.scheduler.Task
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.lang.invoke.MethodHandles
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.time.Duration

@Plugin("bouncer")
class SpongeBouncerPlugin @Inject constructor(
	private val pluginContainer: PluginContainer,
	private val game: Game,
	private val eventManager: EventManager,
	@DefaultConfig(sharedRoot = false) private val configLoader: ConfigurationLoader<CommentedConfigurationNode>)
{
	private lateinit var config: PluginConfig

	private lateinit var bouncer: BouncerAPI
	private lateinit var bouncerServer: IBouncerServer

	@Listener
	fun onConstruct(event: ConstructPluginEvent)
	{
		val node: CommentedConfigurationNode = this.configLoader.load()

		this.config = node.require(PluginConfig::class.java)
		this.configLoader.save(node)

		this.bouncer = SpongeBouncerAPI(this.pluginContainer, event.game(), System.getenv("BOUNCER_ADDRESS") ?: this.config.apiUrl)
	}

	@Listener(order = Order.PRE)
	fun onStarted(event: StartedEngineEvent<Server>)
	{
		this.bouncer.installShutdownSignal()

		val address: InetSocketAddress = this.game.server().boundAddress().get()

		val info = IBouncerServerInfo.of(
			this.config.name,
			this.config.group,
			this.config.type,
			InetSocketAddress.createUnresolved(
				System.getenv("SERVER_IP") ?: address.hostString.ifEmpty()
				{
					DatagramSocket().use()
					{ socket ->
						socket.connect(InetAddress.getByName("1.1.1.1"), 53)
						return@use socket.localAddress.hostAddress
					}
				},
				address.port),
			maxMemory = (Runtime.getRuntime().maxMemory() / 1024L / 1024L).toInt())

		val lookup: MethodHandles.Lookup = MethodHandles.lookup()

		if (this.config.fallback)
		{
			this.bouncerServer = this.bouncer.serverManager.registerServer(IBouncerServerOptions.of(info,
				IBouncerScopeListener.of(this.pluginContainer, { server -> FallbackServerListener.Accept(server) }, lookup)))
		}
		else
		{
			this.bouncerServer = this.bouncer.serverManager.registerServer(IBouncerServerOptions.of(info,
				IBouncerScopeListener.of(this.pluginContainer, { _ -> FallbackServerListener.Refuse() }, lookup)))
		}

		this.bouncer.serverManager.defaultServer = this.bouncerServer
		this.bouncer.serverManager.setFallback(this.bouncerServer)

		this.eventManager.registerListeners(this.pluginContainer, ConnectionListener((this.bouncer as SpongeBouncerAPI).userManager, this.bouncer.serverManager as AbstractServerManager), lookup)
			.registerListeners(this.pluginContainer, CommandListener(this.bouncer), lookup)

		this.game.asyncScheduler().submit(
			Task.builder()
				.plugin(this.pluginContainer)
				.delay(Duration.ofSeconds(1))
				.interval(Duration.ofSeconds(1))
				.execute()
				{ _ ->
					this.bouncerServer.heartbeat(
						tps = (this.game.server().ticksPerSecond() * 100).toInt(),
						memory = ((Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024L / 1024L).toInt()
					)
				}.build())
	}

	@Listener
	fun onStopping(event: StoppingEngineEvent<Server>)
	{
		this.eventManager.unregisterListeners(this.pluginContainer)

		// this.bouncer.serverManager.unregisterServer(this.bouncerServer)
	}

	@Listener
	fun onStopped(event: StoppedGameEvent)
	{
		this.bouncer.shutdownNow()
	}
}
