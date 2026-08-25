package io.quut.bouncer.omnivisor.velocity.universe

import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisor
import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisorProvider
import net.kyori.adventure.key.Key
import java.util.function.BiConsumer

class OmnivisorUniverseSupervisorProvider : IDistributedUniverseSupervisorProvider
{
	override fun provide(register: BiConsumer<Key, IDistributedUniverseSupervisor>)
	{
		register.accept(Key.key("bouncer", "omnivisor"), OmnivisorUniverseSupervisor())
	}
}
