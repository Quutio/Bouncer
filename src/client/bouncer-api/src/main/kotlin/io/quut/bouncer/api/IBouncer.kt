package io.quut.bouncer.api

import io.quut.bouncer.api.server.IBouncerServerManager
import io.quut.bouncer.api.universe.IBouncerUniverseManager

interface IBouncer
{
	val serverManager: IBouncerServerManager
	val universeManager: IBouncerUniverseManager
}
