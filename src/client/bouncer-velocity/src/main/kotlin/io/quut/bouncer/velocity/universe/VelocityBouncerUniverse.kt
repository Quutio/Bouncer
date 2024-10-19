package io.quut.bouncer.velocity.universe

import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.universe.BouncerUniverse
import io.quut.bouncer.velocity.server.VelocityBouncerServer

internal class VelocityBouncerUniverse(server: VelocityBouncerServer, options: IBouncerUniverseOptions) : BouncerUniverse<VelocityBouncerServer, VelocityBouncerUniverse>(server, options)
