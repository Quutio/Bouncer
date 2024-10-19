package io.quut.bouncer.bukkit.server

import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.bukkit.universe.BukkitBouncerUniverse
import io.quut.bouncer.common.server.BouncerServer

internal class BukkitBouncerServer(serverManager: BukkitBouncerServerManager, info: IBouncerServerInfo) : BouncerServer<BukkitBouncerServer, BukkitBouncerUniverse>(serverManager, info)
{
	override fun createUniverse(options: IBouncerUniverseOptions): BukkitBouncerUniverse
	{
		TODO("Not yet implemented")
	}
}
