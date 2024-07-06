package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScopeListener
import java.util.Collections

interface IBouncerServerOptions
{
	val info: IBouncerServerInfo
	val listeners: Collection<IBouncerScopeListener<IBouncerServer>>

	companion object
	{
		@JvmStatic
		fun of(info: IBouncerServerInfo, vararg listeners: IBouncerScopeListener<IBouncerServer>): IBouncerServerOptions =
			Impl(info, Collections.unmodifiableCollection(listeners.toList()))
	}

	private class Impl(
		override val info: IBouncerServerInfo,
		override val listeners: Collection<IBouncerScopeListener<IBouncerServer>>) : IBouncerServerOptions
}
