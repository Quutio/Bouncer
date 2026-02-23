package io.quut.bouncer.api

import io.quut.bouncer.api.server.IDistributedServerManager
import io.quut.bouncer.api.universe.IDistributedUniverseManager

interface IBouncer
{
	val serverManager: IDistributedServerManager
	val universeManager: IDistributedUniverseManager
}
