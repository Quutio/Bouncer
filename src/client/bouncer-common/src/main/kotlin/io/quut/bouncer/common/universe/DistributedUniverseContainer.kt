package io.quut.bouncer.common.universe

import io.quut.bouncer.api.unit.IDistributedUnitState
import io.quut.bouncer.api.universe.IDistributedUniverseContainer
import java.util.UUID

internal class DistributedUniverseContainer(private val instance: DistributedUniverse) : IDistributedUniverseContainer
{
	override var state: IDistributedUnitState
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
		TODO("Not yet implemented")
	}
}
