package io.quut.bouncer.api.universe

import java.util.concurrent.CompletableFuture

interface IDistributedUniverseManager
{
	fun join(request: IDistributedUniverseJoinRequest): CompletableFuture<Boolean>
}
