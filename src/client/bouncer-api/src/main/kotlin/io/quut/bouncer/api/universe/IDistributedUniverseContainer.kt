package io.quut.bouncer.api.universe

import io.quut.bouncer.api.node.IDistributedNodeContainer
import io.quut.bouncer.api.node.IDistributedNodeState

interface IDistributedUniverseContainer : IDistributedNodeContainer
{
	override var state: IDistributedNodeState
}
