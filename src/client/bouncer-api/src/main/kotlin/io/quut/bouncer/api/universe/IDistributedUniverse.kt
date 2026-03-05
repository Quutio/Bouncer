package io.quut.bouncer.api.universe

import io.quut.bouncer.api.server.IDistributedServer
import io.quut.bouncer.api.node.IDistributedNode

interface IDistributedUniverse : IDistributedNode
{
	val server: IDistributedServer
	val info: IDistributedUniverseInfo
}
