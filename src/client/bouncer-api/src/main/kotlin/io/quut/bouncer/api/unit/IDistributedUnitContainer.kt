package io.quut.bouncer.api.unit

import java.util.UUID

interface IDistributedUnitContainer
{
	val state: IDistributedUnitState

	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun close()
}
