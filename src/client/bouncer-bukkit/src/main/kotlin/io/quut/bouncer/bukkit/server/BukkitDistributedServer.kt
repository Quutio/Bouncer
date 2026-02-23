package io.quut.bouncer.bukkit.server

import io.quut.bouncer.api.server.IDistributedServerInfo
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.bukkit.universe.BukkitDistributedUniverse
import io.quut.bouncer.common.server.DistributedServer

internal class BukkitDistributedServer(info: IDistributedServerInfo) : DistributedServer<BukkitDistributedServer, BukkitDistributedUniverse>(info)
{
	override fun createUniverse(options: IDistributedUniverseOptions): BukkitDistributedUniverse
	{
		TODO("Not yet implemented")
	}
}
