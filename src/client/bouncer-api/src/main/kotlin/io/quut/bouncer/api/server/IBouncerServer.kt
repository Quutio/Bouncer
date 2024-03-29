package io.quut.bouncer.api.server

import java.util.UUID

interface IBouncerServer
{
	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun heartbeat(tps: Int? = null, memory: Int? = null)
}
