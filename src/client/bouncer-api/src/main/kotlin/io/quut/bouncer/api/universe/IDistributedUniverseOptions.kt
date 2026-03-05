package io.quut.bouncer.api.universe

import io.quut.bouncer.api.node.IDistributedNodeState

interface IDistributedUniverseOptions
{
	val info: IDistributedUniverseInfo
	val state: IDistributedNodeState

	companion object
	{
		@JvmStatic
		fun of(info: IDistributedUniverseInfo, state: IDistributedNodeState): IDistributedUniverseOptions = Impl(info, state)
	}

	private class Impl(override var info: IDistributedUniverseInfo, override val state: IDistributedNodeState) : IDistributedUniverseOptions
}
