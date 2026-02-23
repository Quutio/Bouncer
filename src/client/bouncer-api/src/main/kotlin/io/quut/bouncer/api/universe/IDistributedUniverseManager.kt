package io.quut.bouncer.api.universe

interface IDistributedUniverseManager
{
	fun join(request: IDistributedUniverseJoinRequest)
}
