package io.quut.bouncer.api.universe.supervisor

import net.kyori.adventure.key.Key
import java.util.function.BiConsumer

interface IDistributedUniverseSupervisorProvider
{
	fun provide(register: BiConsumer<Key, IDistributedUniverseSupervisor>)
}
