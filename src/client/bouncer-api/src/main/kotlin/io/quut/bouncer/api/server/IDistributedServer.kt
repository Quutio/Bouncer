package io.quut.bouncer.api.server

import io.quut.bouncer.api.node.IDistributedNode

interface IDistributedServer : IDistributedNode
{
	val info: IDistributedServerInfo
}
