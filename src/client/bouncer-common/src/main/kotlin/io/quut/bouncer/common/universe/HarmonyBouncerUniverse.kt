package io.quut.bouncer.common.universe

import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.api.universe.IBouncerUniverseStage
import io.quut.bouncer.api.universe.IBouncerUniverseStageOptions
import io.quut.bouncer.common.server.HarmonyBouncerServer
import io.quut.harmony.api.IHarmonyEventListener
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyEventManager.IBuilder.Companion.parentMapping
import io.quut.harmony.api.IHarmonyScopeOptions

abstract class HarmonyBouncerUniverse<TServer, TUniverse>(server: TServer, options: IBouncerUniverseOptions) : BouncerUniverse<TServer, TUniverse>(server, options)
	where TServer : HarmonyBouncerServer<TServer, TUniverse>, TUniverse : HarmonyBouncerUniverse<TServer, TUniverse>
{
	internal lateinit var stageEventManager: IHarmonyEventManager<IBouncerUniverseStage<*>>

	override fun init()
	{
		this.stageEventManager = this.createEventManager()
			.parentMapping { i: HarmonyBouncerUniverse<*, *> -> i.stage }
			.build()

		super.init()
	}

	protected abstract fun createEventManager(): IHarmonyEventManager.IBuilder<IBouncerUniverseStage<*>>

	override fun endStage(stage: IBouncerUniverseStage<*>)
	{
		this.stageEventManager.unregisterScope(stage)

		super.endStage(stage)
	}

	override fun <T : IBouncerUniverseStage<T>> beginStage(stage: T, options: IBouncerUniverseStageOptions<T>)
	{
		super.beginStage(stage, options)

		this.stageEventManager.registerScope(stage, IHarmonyScopeOptions.of(
			listeners = options.listeners.map { listener -> IHarmonyEventListener.of(stage.javaClass, listener.plugin, { stage: T -> listener.listener.apply(this, stage) }, listener.lookup) }.toTypedArray()))
	}

	override fun unregister()
	{
		super.unregister()

		this.server.eventManager.unregisterScope(this)
	}
}
