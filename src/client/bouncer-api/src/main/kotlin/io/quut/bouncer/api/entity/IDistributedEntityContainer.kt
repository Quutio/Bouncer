package io.quut.bouncer.api.entity

import java.util.UUID

interface IDistributedEntityContainer
{
	val state: IDistributedEntityState

	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun close()
}
