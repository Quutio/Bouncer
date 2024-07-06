package io.quut.bouncer.common.game

import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameInfo
import io.quut.bouncer.api.game.IBouncerGameStageType
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.harmony.api.IHarmonyEventListener
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyScopeOptions

abstract class AbstractBouncerGame(internal val server: AbstractBouncerServer, internal val info: IBouncerGameInfo) : RegisteredBouncerScope(), IBouncerGame
{
	internal lateinit var eventManager: IHarmonyEventManager<Any>

	var stage: Any? = null

	override val mutex: Any
		get() = this.server

	protected fun init()
	{
		this.eventManager = this.createEventManager()
	}

	protected abstract fun createEventManager(): IHarmonyEventManager<Any>

	override fun <T : Any> switchStage(type: IBouncerGameStageType<T>)
	{
		if (this.stage != null)
		{
			this.eventManager.unregisterScope(this.stage!!)
		}

		val stage = type.factory(this)

		this.stage = stage

		this.eventManager.registerScope(stage, IHarmonyScopeOptions.of(
			listeners = type.listeners.map { listener -> IHarmonyEventListener.of(stage.javaClass, listener.plugin, { stage: T -> listener.listener(this, stage) }, listener.lookup) }.toTypedArray()))
	}

	override fun nextStage()
	{
	}

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterGame(this, sessionData)
	}
}
