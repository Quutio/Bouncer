package io.quut.bouncer.sponge

import com.google.inject.Inject
import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerHeartbeat
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.common.BouncerDefaultServer
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import io.quut.bouncer.sponge.listeners.FallbackServerListener
import io.quut.bouncer.sponge.listeners.IBouncerListener
import io.quut.bouncer.sponge.server.SpongeDistributedServerManager
import org.spongepowered.api.Game
import org.spongepowered.api.Server
import org.spongepowered.api.event.EventManager
import org.spongepowered.api.scheduler.Task
import org.spongepowered.plugin.PluginContainer
import java.net.InetSocketAddress
import java.time.Duration
import kotlin.jvm.optionals.getOrNull

internal class SpongeBouncerDefaultServer @Inject constructor(
	private val container: PluginContainer,
	private val game: Game,
	private val pluginInfo: SpongeBouncerPluginInfo,
	private val server: Server,
	private val eventManager: EventManager,
	private val listeners: Set<IBouncerListener>,
	plugin: SpongeBouncerPlugin,
	serverManager: SpongeDistributedServerManager) : BouncerDefaultServer(plugin, serverManager)
{
	private var serverContainer: IDistributedServerContainer? = null

	internal fun load()
	{
		if (!this.game.isClientAvailable)
		{
			this.installShutdownSignal()
		}

		this.listeners.forEach { listener -> this.eventManager.registerListeners(this.container, listener, this.pluginInfo.lookup) }

		this.serverContainer = this.createDefaultServer()
	}

	internal fun enable()
	{
		val boundAddress: InetSocketAddress = this.server.boundAddress().getOrNull() ?: return
		val address: InetSocketAddress = InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(boundAddress.hostString), boundAddress.port)

		this.serverContainer?.state = IDistributedServerState.running(address, this.server.maxPlayers())
	}

	internal fun disable()
	{
		this.eventManager.unregisterListeners(this.container)

		this.serverContainer?.close()
		this.serverContainer = null
	}

	override fun defaultServerOptions(info: IDistributedServerInfo): IDistributedServerOptions =
		IDistributedServerOptions.of(info, IDistributedServerState.starting(maxPlayers = this.server.maxPlayers()))

	override fun defaultServerCreated(server: IDistributedServerContainer)
	{
		this.server.streamOnlinePlayers().forEach { player -> server.confirmJoin(player.uniqueId()) }

		this.eventManager.registerListeners(this.container, FallbackServerListener.Accept(server))
	}

	override fun installHeartbeat(runnable: Runnable)
	{
		this.game.asyncScheduler().submit(Task.builder()
			.plugin(this.container)
			.delay(Duration.ofSeconds(1))
			.interval(Duration.ofSeconds(1))
			.execute(runnable)
			.build())
	}

	override fun heartbeat(builder: IDistributedServerHeartbeat.IBuilder)
	{
		builder.tps(this.game.server().ticksPerSecond())
	}

	override fun onShutdownSignal()
	{
		this.server.shutdown()
	}
}
