package io.quut.bouncer.api.server

import io.quut.bouncer.api.node.IDistributedNodeState
import net.kyori.adventure.key.Key
import java.net.InetSocketAddress

interface IDistributedServerState : IDistributedNodeState
{
	val address: InetSocketAddress?

	companion object
	{
		fun of(type: Key, address: InetSocketAddress? = null, maxPlayers: Int? = null): IDistributedServerState = Impl(type, address, maxPlayers)

		fun starting(address: InetSocketAddress? = null, maxPlayers: Int? = null): IDistributedServerState = this.of(IDistributedNodeState.STARTING_KEY, address, maxPlayers)
		fun running(address: InetSocketAddress? = null, maxPlayers: Int? = null): IDistributedServerState = this.of(IDistributedNodeState.RUNNING_KEY, address, maxPlayers)
		fun stopping(): IDistributedServerState = this.of(IDistributedNodeState.STOPPING_KEY)
	}

	private class Impl(override val type: Key, override val address: InetSocketAddress?, override val maxPlayers: Int?) : IDistributedServerState
}
