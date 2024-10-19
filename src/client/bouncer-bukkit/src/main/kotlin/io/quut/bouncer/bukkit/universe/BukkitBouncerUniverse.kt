package io.quut.bouncer.bukkit.universe

import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.bukkit.server.BukkitBouncerServer
import io.quut.bouncer.common.universe.BouncerUniverse

internal class BukkitBouncerUniverse(server: BukkitBouncerServer, options: IBouncerUniverseOptions) : BouncerUniverse<BukkitBouncerServer, BukkitBouncerUniverse>(server, options)
