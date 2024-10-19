package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.universe.IBouncerUniverse
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import java.util.UUID

interface IBouncerServer : IBouncerScope
{
	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun heartbeat(heartbeat: IBouncerServerHeartbeat)

	fun registerUniverse(options: IBouncerUniverseOptions): IBouncerUniverse
	fun unregisterUniverse(universe: IBouncerUniverse)
}
