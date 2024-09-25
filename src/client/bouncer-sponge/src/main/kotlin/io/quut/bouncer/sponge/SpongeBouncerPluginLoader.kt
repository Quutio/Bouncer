package io.quut.bouncer.sponge

import com.google.inject.AbstractModule
import com.google.inject.Inject
import io.quut.bouncer.common.helpers.ServerInfoHelpers
import io.quut.bouncer.sponge.listeners.CommandListener
import io.quut.bouncer.sponge.listeners.ConnectionListener
import org.spongepowered.api.Game
import org.spongepowered.api.Server
import org.spongepowered.api.event.EventManager
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.lifecycle.ConstructPluginEvent
import org.spongepowered.api.event.lifecycle.StartedEngineEvent
import org.spongepowered.api.event.lifecycle.StoppedGameEvent
import org.spongepowered.api.event.lifecycle.StoppingEngineEvent
import org.spongepowered.plugin.PluginContainer
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.net.InetSocketAddress

@Plugin("bouncer")
class SpongeBouncerPluginLoader @Inject constructor(
	private val pluginContainer: PluginContainer,
	private val game: Game,
	private val eventManager: EventManager)
{
	private lateinit var bouncer: SpongeBouncerPlugin

	@Listener
	private fun onConstructPlugin(event: ConstructPluginEvent)
	{
		this.bouncer.load()
	}

	@Listener(order = Order.PRE)
	private fun onStartedEngineServer(event: StartedEngineEvent<Server>)
	{
		val address: InetSocketAddress = this.game.server().boundAddress().get()

		this.bouncer.enable(InetSocketAddress.createUnresolved(ServerInfoHelpers.resolveHostAddress(address.hostString), address.port))

		this.bouncer.registerListener(ConnectionListener::class.java)
		this.bouncer.registerListener(CommandListener::class.java)
	}

	@Listener
	private fun onStoppingEngineServer(event: StoppingEngineEvent<Server>)
	{
		this.eventManager.unregisterListeners(this.pluginContainer)

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
		}
	}
}
