package io.quut.bouncer.common.universe

import io.quut.bouncer.api.entity.IDistributedEntityState
import io.quut.bouncer.api.universe.IDistributedUniverseContainer
import java.util.UUID

internal class DistributedUniverseContainer(
	private val instance: DistributedUniverse,
	private val closeCallback: (DistributedUniverse) -> Unit) : IDistributedUniverseContainer
{
	override var state: IDistributedEntityState
		get() = this.instance.state
		set(value)
		{
			this.instance.state = value
		}

	override fun confirmJoin(uniqueId: UUID)
	{
		TODO("Not yet implemented")
	}

	override fun confirmLeave(uniqueId: UUID)
	{
		TODO("Not yet implemented")
	}

	override fun close()
	{
		this.closeCallback(this.instance)
	}
}
