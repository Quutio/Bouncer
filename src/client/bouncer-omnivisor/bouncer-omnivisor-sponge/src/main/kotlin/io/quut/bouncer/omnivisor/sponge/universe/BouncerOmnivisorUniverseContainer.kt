package io.quut.bouncer.omnivisor.sponge.universe

import io.quut.bouncer.api.entity.IDistributedEntityState
import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.universe.IDistributedUniverseContainer
import io.quut.bouncer.api.universe.IDistributedUniverseOptions
import io.quut.bouncer.api.universe.IDistributedUniverseProvider
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceOptions
import io.quut.bouncer.omnivisor.sponge.util.Const
import io.quut.omnivisor.api.universe.IUniverseContainer
import java.util.concurrent.CompletableFuture

internal class BouncerOmnivisorUniverseContainer(
	private val server: IDistributedServerContainer,
	private val container: IUniverseContainer) : IDistributedUniverseProvider
{
	private val universes: MutableSet<IDistributedUniverseContainer> = mutableSetOf()

	private val lock: Any = Any()

	@Volatile
	private var closed: Boolean = false

	override fun registerUniverse(options: IDistributedUniverseOptions): IDistributedUniverseContainer = synchronized(this.lock)
	{
		if (this.closed)
		{
			throw IllegalStateException("Closed")
		}

		val universe: IDistributedUniverseContainer = this.server.registerUniverse(options,
			IDistributedUniverseSupervisorInstanceOptions.of(
				IDistributedUniverseSupervisorInstanceInfo.of(Const.OMNIVISOR_KEY,
					mapOf(Pair("universe", this.container.info.id.toString())))))

		this.universes.add(universe)

		return Container(universe)
	}

	internal fun stop(): CompletableFuture<Void>
	{
		synchronized(this.lock)
		{
			this.closed = true

			this.universes.forEach { u -> u.state = IDistributedEntityState.stopping() }
		}

		return CompletableFuture.completedFuture(null)
	}

	internal fun close(): CompletableFuture<Void>
	{
		synchronized(this.lock) { this.universes.forEach { u -> u.close() } }

		return CompletableFuture.completedFuture(null)
	}

	private inner class Container(private val container: IDistributedUniverseContainer) : IDistributedUniverseContainer by container
	{
		override fun close()
		{
			synchronized(this@BouncerOmnivisorUniverseContainer.lock)
			{
				this@BouncerOmnivisorUniverseContainer.universes.remove(this.container)
			}

			this.container.close()
		}
	}
}
