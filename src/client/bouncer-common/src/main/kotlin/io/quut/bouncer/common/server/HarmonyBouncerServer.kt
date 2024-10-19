package io.quut.bouncer.common.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.universe.IBouncerUniverse
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.universe.HarmonyBouncerUniverse
import io.quut.harmony.api.IHarmonyEventListener
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyScopeOptions

abstract class HarmonyBouncerServer<TServer, TUniverse>(serverManager: AbstractServerManager<TServer, TUniverse>, info: IBouncerServerInfo) : BouncerServer<TServer, TUniverse>(serverManager, info)
	where TServer : HarmonyBouncerServer<TServer, TUniverse>, TUniverse : HarmonyBouncerUniverse<TServer, TUniverse>
{
	internal lateinit var eventManager: IHarmonyEventManager<IBouncerScope>
		private set

	internal fun attachEventManager(eventManager: IHarmonyEventManager<IBouncerScope>)
	{
		this.eventManager = eventManager
	}

	override fun registerUniverse(options: IBouncerUniverseOptions, universe: TUniverse)
	{
		this.eventManager.registerScope(universe, IHarmonyScopeOptions.of(
			child = universe.stageEventManager,
			listeners = options.listeners.map { listener -> IHarmonyEventListener.of(listener.plugin, { universe: IBouncerUniverse -> listener.listener.apply(universe) }, listener.lookup) }.toTypedArray()))
	}

	override fun unregister()
	{
		super.unregister()

		this.eventManager.unregisterScope(this)
	}
}
