package io.quut.bouncer.omnivisor.sponge

import com.google.inject.Inject
import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.server.IDistributedServerManager
import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.api.server.IDistributedServerState
import io.quut.bouncer.api.universe.IDistributedUniverseProvider
import io.quut.bouncer.omnivisor.sponge.universe.BouncerOmnivisorUniverseContainer
import io.quut.bouncer.omnivisor.sponge.util.Const
import io.quut.bouncer.omnivisor.sponge.util.Options
import io.quut.omnivisor.api.universe.IUniverseArchetype
import io.quut.omnivisor.api.universe.IUniverseContext
import io.quut.omnivisor.api.universe.IUniverseTemplate.IStepBuilder.Companion.complete
import io.quut.omnivisor.api.universe.IUniverseTemplate.IStepBuilder.Companion.event
import io.quut.omnivisor.api.universe.IUniverseTemplate.IStepBuilder.Companion.provideFactory
import io.quut.omnivisor.api.universe.event.IUniverseStartedEvent
import io.quut.omnivisor.api.universe.event.IUniverseStoppedEvent
import io.quut.omnivisor.api.universe.event.IUniverseStoppingEvent
import io.quut.omnivisor.api.universe.event.UniverseEventPriority
import org.spongepowered.api.Game
import org.spongepowered.api.Server
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.lifecycle.RegisterRegistryValueEvent
import org.spongepowered.api.registry.DefaultedRegistryType
import org.spongepowered.plugin.builtin.jvm.Plugin
import java.util.function.Function
import kotlin.jvm.optionals.getOrNull

@Plugin(Const.PLUGIN_ID)
class SpongeBouncerOmnivisorPluginLoader @Inject internal constructor(
	private val game: Game,
	private val bouncerServerManager: IDistributedServerManager,
	private val universeArchetypeRegistry: DefaultedRegistryType<IUniverseArchetype<*, *>>)
{
	@Listener
	private fun onRegisterRegistryValueServer(event: RegisterRegistryValueEvent.EngineScoped<Server>)
	{
		fun createUniverseProvider(server: IDistributedServerContainer): Function<IUniverseContext<*, *>, IDistributedUniverseProvider> =
			{ context ->
				BouncerOmnivisorUniverseContainer(server, context.container)
					.apply { context.event(IUniverseStoppingEvent::class.java, UniverseEventPriority.PRE) { this.stop() } }
					.apply { context.event(IUniverseStoppedEvent::class.java, UniverseEventPriority.POST) { this.close() } }
			}

		event.registry(this.universeArchetypeRegistry)
		{ s ->
			s.register(Const.key("server"), IUniverseArchetype.builder<BouncerMultiverse, BouncerMultiverseConfig>()
				.options(Options.Holder.SCHEMA)
				.configLoader(Options::loadConfig)
				.virtualMultiverse(node = true)
				.step()
				{ config ->
					val server: IDistributedServerContainer = this.bouncerServerManager.registerServer(
						IDistributedServerOptions.of(IDistributedServerInfo.of(config.name, config.group, config.type),
							IDistributedServerState.starting(config.address ?: this.game.server().boundAddress().getOrNull())))

					BouncerMultiverse(server)
				}
				.provideFactory { s -> createUniverseProvider(s.server) }
				.event(UniverseEventPriority.POST) { _: IUniverseStartedEvent, s -> s.server.state = IDistributedServerState.running(s.server.state.address) }
				.complete()
				.build())
		}
	}
}
