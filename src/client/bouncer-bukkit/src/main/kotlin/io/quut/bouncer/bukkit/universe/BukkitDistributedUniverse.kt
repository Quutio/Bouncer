package io.quut.bouncer.bukkit.universe

import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.bukkit.server.BukkitDistributedServer
import io.quut.bouncer.common.universe.DistributedUniverse

internal class BukkitDistributedUniverse(server: BukkitDistributedServer, options: IDistributedUniverseOptions) : DistributedUniverse<BukkitDistributedServer, BukkitDistributedUniverse>(server, options)
