package io.quut.bouncer.common.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.universe.HarmonyBouncerUniverse
import io.quut.bouncer.common.user.UserManager
import io.quut.harmony.api.IHarmonyEventListener
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyScopeOptions

abstract class HarmonyServerManager<TServer, TUniverse>(networkManager: NetworkManager, userManager: UserManager) : AbstractServerManager<TServer, TUniverse>(networkManager, userManager)
	where TServer : HarmonyBouncerServer<TServer, TUniverse>, TUniverse : HarmonyBouncerUniverse<TServer, TUniverse>
{
	private lateinit var eventManager: IHarmonyEventManager<IBouncerScope>

	protected abstract fun createEventManager(): IHarmonyEventManager.IBuilder<IBouncerScope>

	override fun init()
	{
		this.eventManager = this.createEventManager().build()
	}

	override fun registerServer(options: IBouncerServerOptions, server: TServer)
	{
		server.attachEventManager(this.eventManager)

		this.eventManager.registerScope(server, IHarmonyScopeOptions.of(
			listeners = options.listeners.map { listener -> IHarmonyEventListener.of(listener.plugin, { server: IBouncerServer -> listener.listener.apply(server) }, listener.lookup) }.toTypedArray()))
	}
}
