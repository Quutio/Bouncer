package io.quut.bouncer.sponge.universe

import io.quut.bouncer.api.universe.IBouncerUniverseOptions
import io.quut.bouncer.common.universe.BouncerUniverse
import io.quut.bouncer.sponge.server.SpongeBouncerServer

internal class SpongeBouncerUniverse(server: SpongeBouncerServer, options: IBouncerUniverseOptions) : BouncerUniverse<SpongeBouncerServer, SpongeBouncerUniverse>(server, options)
