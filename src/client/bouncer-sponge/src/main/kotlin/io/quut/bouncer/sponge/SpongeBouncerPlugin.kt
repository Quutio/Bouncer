package io.quut.bouncer.sponge

import com.google.inject.Inject
import io.quut.bouncer.api.IBouncerScopeListener
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerHeartbeat
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.sponge.config.PluginConfig
import io.quut.bouncer.sponge.listeners.FallbackServerListener
import io.quut.bouncer.sponge.server.SpongeBouncerServerManager
import org.spongepowered.api.Game
import org.spongepowered.api.scheduler.Task
import org.spongepowered.configurate.CommentedConfigurationNode
import java.time.Duration

internal class SpongeBouncerPlugin @Inject constructor(
	private val plugin: ISpongeBouncerPlugin,
	private val game: Game,
	networkManager: NetworkManager,
	serverManager: SpongeBouncerServerManager) : BouncerPlugin(networkManager, serverManager)
{
	override lateinit var config: PluginConfig

	override fun loadConfig()
	{
		val node: CommentedConfigurationNode = this.plugin.configLoader.load()

		this.plugin.configLoader.save(node)

		this.config = node.require(PluginConfig::class.java)
	}

	override fun defaultServerOptions(info: IBouncerServerInfo): IBouncerServerOptions
	{
		val listener: IBouncerScopeListener<IBouncerServer>
		if (this.config.fallback)
		{
			listener = IBouncerScopeListener.of(this.plugin.container, { server -> FallbackServerListener.Accept(server) }, this.plugin.lookup)
		}
		else
		{
			listener = IBouncerScopeListener.of(this.plugin.container, { _ -> FallbackServerListener.Refuse() }, this.plugin.lookup)
		}

		return IBouncerServerOptions.of(info, listener)
	}

	override fun defaultServerCreated(server: IBouncerServer)
	{
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.game.asyncScheduler().submit(Task.builder()
			.plugin(this.plugin.container)
			.delay(Duration.ofSeconds(1))
			.interval(Duration.ofSeconds(1))
			.execute(runnable)
			.build())
	}

	override fun heartbeat(builder: IBouncerServerHeartbeat.IBuilder)
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
}
