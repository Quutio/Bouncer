package io.quut.bouncer.api.server

import java.util.Collections

interface IBouncerServerWatchRequest
{
	val filter: Collection<IBouncerServerFilter>
	val eventHandler: IBouncerServerEventHandler

	companion object
	{
		@JvmStatic
		fun of(eventHandler: IBouncerServerEventHandler, vararg filters: IBouncerServerFilter): IBouncerServerWatchRequest =
			Impl(Collections.unmodifiableCollection(filters.toList()), eventHandler)
	}

	private class Impl(override val filter: Collection<IBouncerServerFilter>, override val eventHandler: IBouncerServerEventHandler) : IBouncerServerWatchRequest
}
