package io.quut.bouncer.sponge

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.PrivateModule
import com.google.inject.Scopes
import com.google.inject.multibindings.Multibinder
import com.google.inject.name.Names
import io.quut.bouncer.api.IBouncer
import io.quut.bouncer.api.server.IDistributedServerManager
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.sponge.listeners.CommandListener
import io.quut.bouncer.sponge.listeners.ConnectionListener
import io.quut.bouncer.sponge.listeners.IBouncerListener
import io.quut.bouncer.sponge.server.SpongeDistributedServerManager
import io.quut.bouncer.sponge.user.SpongeUserManager
import io.quut.bouncer.sponge.utils.Const
import org.spongepowered.api.Server
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent
import org.spongepowered.api.event.lifecycle.RegisterChannelEvent
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StartingEngineEvent
import org.spongepowered.api.event.lifecycle.StoppedGameEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.api.network.channel.raw.RawDataChannel
import org.spongepowered.plugin.builtin.jvm.Plugin

@Plugin(Const.NAMESPACE)
class SpongeBouncerPluginLoader @Inject internal constructor(
	private val injector: Injector,
	private val plugin: SpongeBouncerPlugin)
{
	private lateinit var loginChannel: RawDataChannel

	private var defaultServer: SpongeBouncerDefaultServer? = null

	@Listener
	private fun onConstructPlugin(event: ConstructPluginEvent)
	{
		this.plugin.load()
	}

	@Listener
	private fun onRegisterChannel(event: RegisterChannelEvent)
	{
		this.loginChannel = event.register(Const.LOGIN_CHANNEL_KEY, RawDataChannel::class.java)
	}

	@Listener(order = Order.PRE)
	private fun onStartingEngineServer(event: StartingEngineEvent<Server>)
	{
		val injector: Injector = this.injector.createChildInjector(ServerModule(event.engine(), this.loginChannel))

		val defaultServer: SpongeBouncerDefaultServer = injector.getInstance(SpongeBouncerDefaultServer::class.java)
		defaultServer.load()

		this.defaultServer = defaultServer
	}

	@Listener(order = Order.POST)
	private fun onStartedEngineServer(event: StartedEngineEvent<Server>)
	{
		this.defaultServer?.enable()
	}

	@Listener
	private fun onStoppingEngineServer(event: StoppingEngineEvent<Server>)
	{
		this.defaultServer?.disable()
		this.defaultServer = null
	}

	@Listener(order = Order.POST)
	private fun onStoppedGame(event: StoppedGameEvent)
	{
		this.plugin.shutdownNow()
	}

	class Module : PrivateModule()
	{
		override fun configure()
		{
			this.bind(SpongeBouncerPluginInfo::class.java).`in`(Scopes.SINGLETON)
			this.bind(SpongeBouncerPlugin::class.java).`in`(Scopes.SINGLETON)
			this.bind(SpongeDistributedServerManager::class.java).`in`(Scopes.SINGLETON)
			this.bind(NetworkManager::class.java).`in`(Scopes.SINGLETON)
			this.bind(SpongeUserManager::class.java).`in`(Scopes.SINGLETON)

			// Public APIs
			this.bind(IBouncer::class.java).to(SpongeBouncerPlugin::class.java)
			this.bind(IDistributedServerManager::class.java).to(SpongeDistributedServerManager::class.java)

			this.expose(IBouncer::class.java)
			this.expose(IDistributedServerManager::class.java)
		}
	}

	private class ServerModule(private val server: Server, private val loginChannel: RawDataChannel) : AbstractModule()
	{
		override fun configure()
		{
			this.bind(Server::class.java).toInstance(this.server)

			this.bind(RawDataChannel::class.java)
				.annotatedWith(Names.named(Const.LOGIN_CHANNEL))
				.toInstance(this.loginChannel)

			this.bind(SpongeBouncerDefaultServer::class.java).`in`(Scopes.SINGLETON)

			val listeners: Multibinder<IBouncerListener> = Multibinder.newSetBinder(this.binder(), IBouncerListener::class.java)
			listeners.addBinding().to(ConnectionListener::class.java)
			listeners.addBinding().to(CommandListener::class.java)
		}
	}
}
