package io.quut.bouncer.api.node

import java.util.UUID

interface IDistributedNodeContainer
{
	val state: IDistributedNodeState

	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun close()
}
