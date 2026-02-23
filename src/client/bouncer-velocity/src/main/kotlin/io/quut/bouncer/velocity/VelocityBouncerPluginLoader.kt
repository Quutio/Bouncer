package io.quut.bouncer.velocity

import com.google.inject.AbstractModule
import com.google.inject.Inject
import com.google.inject.Injector
import com.google.inject.Scopes
import com.velocitypowered.api.event.EventManager
import com.velocitypowered.api.plugin.Plugin
import com.velocitypowered.api.plugin.PluginContainer
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.velocity.server.DynamicServerEventHandler
import io.quut.bouncer.velocity.server.VelocityDistributedServerManager

@Plugin(id = "bouncer", name = "Bouncer", version = "1.0", url = "https://quut.io", authors = [ "Joni Aromaa (isokissa3)", "Ossi Erkkilä (avaruus1)" ])
class VelocityBouncerPluginLoader @Inject internal constructor(
	private val injector: Injector,
	private val container: PluginContainer,
	private val eventManager: EventManager)
{
	private val boostrap: VelocityBouncerPluginBootstrap = this.injector.createChildInjector(Module())
		.getInstance(VelocityBouncerPluginBootstrap::class.java)

	init
	{
		this.eventManager.register(this.container, this.boostrap)
	}

	private class Module : AbstractModule()
	{
		override fun configure()
		{
			this.bind(NetworkManager::class.java).`in`(Scopes.SINGLETON)
			this.bind(UserManager::class.java).`in`(Scopes.SINGLETON)
			this.bind(VelocityDistributedServerManager::class.java).`in`(Scopes.SINGLETON)
			this.bind(VelocityBouncerPlugin::class.java).`in`(Scopes.SINGLETON)
			this.bind(VelocityBouncerDefaultServer::class.java).`in`(Scopes.SINGLETON)
			this.bind(DynamicServerEventHandler::class.java).`in`(Scopes.SINGLETON)
		}
	}
}
