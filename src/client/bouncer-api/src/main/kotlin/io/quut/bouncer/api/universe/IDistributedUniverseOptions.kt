package io.quut.bouncer.api.universe

import io.quut.bouncer.api.entity.IDistributedEntityState

interface IDistributedUniverseOptions
{
	val info: IDistributedUniverseInfo
	val state: IDistributedEntityState

	companion object
	{
		@JvmStatic
		fun of(info: IDistributedUniverseInfo, state: IDistributedEntityState): IDistributedUniverseOptions = Impl(info, state)
	}

	private class Impl(override var info: IDistributedUniverseInfo, override val state: IDistributedEntityState) : IDistributedUniverseOptions
}
