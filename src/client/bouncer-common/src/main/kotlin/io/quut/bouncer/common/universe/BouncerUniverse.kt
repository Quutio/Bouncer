package io.quut.bouncer.common.universe

import io.quut.bouncer.api.universe.IBouncerUniverse
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.api.universe.IBouncerUniverseStage
import io.quut.bouncer.api.universe.IBouncerUniverseStageOptions
import io.quut.bouncer.api.universe.IBouncerUniverseStageType
import io.quut.bouncer.common.network.RegisteredBouncerScope
import io.quut.bouncer.common.server.BouncerServer

abstract class BouncerUniverse<TServer, TUniverse>(internal val server: TServer, internal val options: IBouncerUniverseOptions) : RegisteredBouncerScope(), IBouncerUniverse
	where TServer : BouncerServer<TServer, TUniverse>, TUniverse : BouncerUniverse<TServer, TUniverse>
{
	internal var stage: IBouncerUniverseStage<*>? = null
		private set

	override val mutex: Any
		get() = this.server

	internal open fun init()
	{
		this.options.stages.firstOrNull()?.let { stage -> this.switchStage(stage) }
	}

	protected open fun endStage(stage: IBouncerUniverseStage<*>)
	{
		stage.end()
	}

	protected open fun <T : IBouncerUniverseStage<T>> beginStage(stage: T, options: IBouncerUniverseStageOptions<T>)
	{
		stage.begin()
	}

	@Suppress("UNCHECKED_CAST")
	override fun <T : IBouncerUniverseStage<T>> switchStage(type: IBouncerUniverseStageType<T>)
	{
		val options: IBouncerUniverseStageOptions<T>? = this.options.stages.find { s -> s.type == type } as? IBouncerUniverseStageOptions<T>
		if (options == null)
		{
			throw IllegalArgumentException("All valid stages must be part of options!")
		}

		this.switchStage(options)
	}

	private fun <T : IBouncerUniverseStage<T>> switchStage(options: IBouncerUniverseStageOptions<T>)
	{
		this.stage?.let(this::endStage)

		val stage: T = options.factory.apply(this)

		this.stage = stage

		this.beginStage(stage, options)
	}

	override fun nextStage()
	{
		val stage: IBouncerUniverseStage<*> = this.stage ?: return

		val nextIndex: Int = this.options.stages.indexOfFirst { s -> s.type === stage.type } + 1
		if (nextIndex >= this.options.stages.size)
		{
			return
		}

		this.switchStage(this.options.stages[nextIndex])
	}

	override fun onUnregistered(sessionData: SessionData)
	{
		sessionData.session.unregisterUniverse(this, sessionData)
	}
}
