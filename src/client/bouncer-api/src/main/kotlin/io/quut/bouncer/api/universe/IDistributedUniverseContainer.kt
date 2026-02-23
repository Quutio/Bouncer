package io.quut.bouncer.api.universe

import io.quut.bouncer.api.unit.IDistributedUnitContainer
import io.quut.bouncer.api.unit.IDistributedUnitState

interface IDistributedUniverseContainer : IDistributedUnitContainer
{
	override var state: IDistributedUnitState
}
