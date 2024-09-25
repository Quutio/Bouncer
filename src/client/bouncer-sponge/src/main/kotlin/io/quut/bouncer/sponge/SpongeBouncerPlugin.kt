package io.quut.bouncer.sponge

import com.google.inject.Inject
import com.google.inject.Injector
import io.quut.bouncer.api.IBouncerScopeListener
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.api.server.IServerHeartbeat
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.sponge.config.PluginConfig
import io.quut.bouncer.sponge.listeners.FallbackServerListener
import io.quut.bouncer.sponge.server.SpongeServerManager
import org.spongepowered.api.Game
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.EventManager
import org.spongepowered.api.scheduler.Task
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.plugin.PluginContainer
import java.lang.invoke.MethodHandles
import java.time.Duration

internal class SpongeBouncerPlugin @Inject constructor(
	private val injector: Injector,
	private val pluginContainer: PluginContainer,
	private val game: Game,
	private val eventManager: EventManager,
	@DefaultConfig(sharedRoot = false) private val configLoader: ConfigurationLoader<CommentedConfigurationNode>,
	serverManager: SpongeServerManager) : BouncerPlugin(serverManager)
{
	private val lookup: MethodHandles.Lookup = MethodHandles.lookup()

	override lateinit var config: PluginConfig

	override fun loadConfig()
	{
		val node: CommentedConfigurationNode = this.configLoader.load()

		this.configLoader.save(node)

		this.config = node.require(PluginConfig::class.java)
	}

	override fun defaultServerOptions(info: IBouncerServerInfo): IBouncerServerOptions
	{
		val listener: IBouncerScopeListener<IBouncerServer>
		if (this.config.fallback)
		{
			listener = IBouncerScopeListener.of(this.pluginContainer, { server -> FallbackServerListener.Accept(server) }, this.lookup)
		}
		else
		{
			listener = IBouncerScopeListener.of(this.pluginContainer, { _ -> FallbackServerListener.Refuse() }, this.lookup)
		}

		return IBouncerServerOptions.of(info, listener)
	}

	override fun defaultServerCreated(server: IBouncerServer)
	{
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.game.asyncScheduler().submit(Task.builder()
			.plugin(this.pluginContainer)
			.delay(Duration.ofSeconds(1))
			.interval(Duration.ofSeconds(1))
			.execute(runnable)
			.build())
	}

	override fun heartbeat(builder: IServerHeartbeat.IBuilder)
	{
		builder.tps(this.game.server().ticksPerSecond())
	}

	override fun onShutdownSignal()
	{
		if (this.game.isServerAvailable)
		{
			this.game.server().shutdown()
		}
	}

	internal fun registerListener(clazz: Class<*>)
	{
		this.eventManager.registerListeners(this.pluginContainer, this.injector.getInstance(clazz), this.lookup)
	}
}
