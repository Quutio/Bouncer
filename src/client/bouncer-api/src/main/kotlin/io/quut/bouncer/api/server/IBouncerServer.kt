package io.quut.bouncer.api.server

import java.util.*

interface IBouncerServer
{
	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)
}
