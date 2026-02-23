package io.quut.bouncer.api.server

import java.util.Collections

interface IDistributedServerWatchRequest
{
	val filter: Collection<IDistributedServerFilter>
	val eventHandler: IDistributedServerEventHandler

	companion object
	{
		@JvmStatic
		fun of(eventHandler: IDistributedServerEventHandler, vararg filters: IDistributedServerFilter): IDistributedServerWatchRequest =
			Impl(Collections.unmodifiableCollection(filters.toList()), eventHandler)
	}

	private class Impl(override val filter: Collection<IDistributedServerFilter>, override val eventHandler: IDistributedServerEventHandler) : IDistributedServerWatchRequest
}
