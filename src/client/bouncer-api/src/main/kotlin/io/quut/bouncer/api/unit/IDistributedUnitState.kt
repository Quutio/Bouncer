package io.quut.bouncer.api.unit

import io.quut.bouncer.api.util.Const
import net.kyori.adventure.key.Key

interface IDistributedUnitState
{
	val type: Key
	val maxPlayers: Int?

	companion object
	{
		val STARTING_KEY: Key = Const.key("starting")
		val RUNNING_KEY: Key = Const.key("running")
		val STOPPING_KEY: Key = Const.key("stopping")

		fun of(type: Key, maxPlayers: Int? = null): IDistributedUnitState = Impl(type, maxPlayers)

		fun starting(maxPlayers: Int? = null): IDistributedUnitState = this.of(this.STARTING_KEY, maxPlayers)
		fun running(maxPlayers: Int? = null): IDistributedUnitState = this.of(this.RUNNING_KEY, maxPlayers)
		fun stopping(): IDistributedUnitState = this.of(this.STOPPING_KEY)
	}

	private class Impl(override val type: Key, override val maxPlayers: Int?) : IDistributedUnitState
}
