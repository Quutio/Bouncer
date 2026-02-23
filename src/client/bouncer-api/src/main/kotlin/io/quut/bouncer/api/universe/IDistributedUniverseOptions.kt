package io.quut.bouncer.api.universe

import io.quut.bouncer.api.unit.IDistributedUnitState

interface IDistributedUniverseOptions
{
	val info: IDistributedUniverseInfo
	val state: IDistributedUnitState

	companion object
	{
		@JvmStatic
		fun of(info: IDistributedUniverseInfo, state: IDistributedUnitState): IDistributedUniverseOptions = Impl(info, state)
	}

	private class Impl(override var info: IDistributedUniverseInfo, override val state: IDistributedUnitState) : IDistributedUniverseOptions
}
