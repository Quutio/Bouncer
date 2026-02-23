package io.quut.bouncer.api.server

import io.quut.bouncer.api.unit.IDistributedUnitState
import net.kyori.adventure.key.Key
import java.net.InetSocketAddress

interface IDistributedServerState : IDistributedUnitState
{
	val address: InetSocketAddress?

	companion object
	{
		fun of(type: Key, address: InetSocketAddress? = null, maxPlayers: Int? = null): IDistributedServerState = Impl(type, address, maxPlayers)

		fun starting(address: InetSocketAddress? = null, maxPlayers: Int? = null): IDistributedServerState = this.of(IDistributedUnitState.STARTING_KEY, address, maxPlayers)
		fun running(address: InetSocketAddress? = null, maxPlayers: Int? = null): IDistributedServerState = this.of(IDistributedUnitState.RUNNING_KEY, address, maxPlayers)
		fun stopping(): IDistributedServerState = this.of(IDistributedUnitState.STOPPING_KEY)
	}

	private class Impl(override val type: Key, override val address: InetSocketAddress?, override val maxPlayers: Int?) : IDistributedServerState
}
