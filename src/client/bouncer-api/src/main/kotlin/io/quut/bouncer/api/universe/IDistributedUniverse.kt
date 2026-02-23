package io.quut.bouncer.api.universe

import io.quut.bouncer.api.server.IDistributedServer
import io.quut.bouncer.api.unit.IDistributedUnit

interface IDistributedUniverse : IDistributedUnit
{
	val server: IDistributedServer
	val info: IDistributedUniverseInfo
}
