package io.quut.bouncer.api.universe

import io.quut.bouncer.api.entity.IDistributedEntity
import io.quut.bouncer.api.server.IDistributedServer

interface IDistributedUniverse : IDistributedEntity
{
	val server: IDistributedServer
	val info: IDistributedUniverseInfo
}
