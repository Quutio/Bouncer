package io.quut.bouncer.sponge

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Singleton
import com.google.inject.multibindings.Multibinder
import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.api.server.IBouncerServerManager
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.sponge.listeners.CommandListener
import io.quut.bouncer.sponge.listeners.ConnectionListener
import io.quut.bouncer.sponge.listeners.IBouncerListener
import io.quut.bouncer.sponge.server.SpongeBouncerServerManager
import io.quut.bouncer.sponge.user.SpongeUserManager
import io.quut.bouncer.sponge.utils.Const
import org.spongepowered.api.Game
import org.spongepowered.api.Server
import org.spongepowered.api.config.DefaultConfig
import org.spongepowered.api.event.EventManager
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StoppedGameEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.api.network.channel.raw.RawDataChannel
import org.spongepowered.configurate.CommentedConfigurationNode
import org.spongepowered.configurate.loader.ConfigurationLoader
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.lang.invoke.MethodHandles
import java.net.InetSocketAddress

@Plugin(Const.NAMESPACE)
class SpongeBouncerPluginLoader @Inject internal constructor(
	override val container: PluginContainer,
	@DefaultConfig(sharedRoot = false) override val configLoader: ConfigurationLoader<CommentedConfigurationNode>,
	private val game: Game,
	private val eventManager: EventManager,
	private val bouncer: SpongeBouncerPlugin,
	private val listeners: Set<IBouncerListener>) : ISpongeBouncerPlugin
{
	override val lookup: MethodHandles.Lookup = MethodHandles.lookup()

	override lateinit var loginChannel: RawDataChannel

	@Listener
	private fun onConstructPlugin(event: ConstructPluginEvent)
	{
		this.bouncer.load()
	}

	@Listener
	private fun onRegisterChannel(event: RegisterChannelEvent)
	{
		this.loginChannel = event.register(Const.LOGIN_CHANNEL_KEY, RawDataChannel::class.java)
	}

	@Listener(order = Order.PRE)
	private fun onStartedEngineServer(event: StartedEngineEvent<Server>)
	{
		val address: InetSocketAddress = this.game.server().boundAddress().get()

		this.bouncer.enable(InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(address.hostString), address.port))

		this.listeners.forEach { listener -> this.eventManager.registerListeners(this.container, listener, this.lookup) }
	}

	@Listener
	private fun onStoppingEngineServer(event: StoppingEngineEvent<Server>)
	{
		this.eventManager.unregisterListeners(this.container)

		if (this.game.isClientAvailable)
		{
			this.bouncer.disable()
		}
	}

	@Listener(order = Order.POST)
	private fun onStoppedGame(event: StoppedGameEvent)
	{
		this.bouncer.shutdownNow()
	}

	class Module : AbstractModule()
	{
		override fun configure()
		{
			this.bind(ISpongeBouncerPlugin::class.java).to(SpongeBouncerPluginLoader::class.java)
			this.bind(SpongeBouncerPlugin::class.java).`in`(Singleton::class.java)
			this.bind(SpongeBouncerServerManager::class.java).`in`(Singleton::class.java)
			this.bind(NetworkManager::class.java).`in`(Singleton::class.java)
			this.bind(SpongeUserManager::class.java).`in`(Singleton::class.java)

			val listeners: Multibinder<IBouncerListener> = Multibinder.newSetBinder(this.binder(), IBouncerListener::class.java)
			listeners.addBinding().to(ConnectionListener::class.java)
			listeners.addBinding().to(CommandListener::class.java)

			// Public APIs
			this.bind(IBouncer::class.java).to(SpongeBouncerPlugin::class.java)
			this.bind(IBouncerServerManager::class.java).to(SpongeBouncerServerManager::class.java)
		}
	}
}
