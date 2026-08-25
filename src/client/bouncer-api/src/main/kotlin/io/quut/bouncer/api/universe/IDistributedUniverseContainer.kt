package io.quut.bouncer.api.universe

import io.quut.bouncer.api.entity.IDistributedEntityContainer
import io.quut.bouncer.api.entity.IDistributedEntityState

interface IDistributedUniverseContainer : IDistributedEntityContainer
{
	override var state: IDistributedEntityState
}
