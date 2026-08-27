package io.quut.bouncer.api.entity

import io.quut.bouncer.api.util.Const
import net.kyori.adventure.key.Key

interface IDistributedEntityState
{
	val type: Key
	val maxPlayers: Int?

	companion object
	{
		val STARTING_KEY: Key = Const.key("starting")
		val RUNNING_KEY: Key = Const.key("running")
		val STOPPING_KEY: Key = Const.key("stopping")

		@JvmStatic
		fun of(type: Key, maxPlayers: Int? = null): IDistributedEntityState = Impl(type, maxPlayers)

		@JvmStatic
		fun starting(maxPlayers: Int? = null): IDistributedEntityState = this.of(this.STARTING_KEY, maxPlayers)

		@JvmStatic
		fun running(maxPlayers: Int? = null): IDistributedEntityState = this.of(this.RUNNING_KEY, maxPlayers)

		@JvmStatic
		fun stopping(): IDistributedEntityState = this.of(this.STOPPING_KEY)
	}

	private class Impl(override val type: Key, override val maxPlayers: Int?) : IDistributedEntityState
}
