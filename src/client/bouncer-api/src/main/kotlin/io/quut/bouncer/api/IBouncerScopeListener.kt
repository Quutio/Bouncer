package io.quut.bouncer.api

import java.lang.invoke.MethodHandles

interface IBouncerScopeListener<T : IBouncerScope>
{
	val plugin: Any

	val listener: (T) -> Any

	val lookup: MethodHandles.Lookup?

	companion object
	{
		@JvmStatic
		@JvmOverloads
		fun <T : IBouncerScope> of(plugin: Any, listener: (T) -> Any, lookup: MethodHandles.Lookup? = null): IBouncerScopeListener<T> =
			Impl(plugin, listener, lookup)
	}

	private class Impl<T : IBouncerScope>(
		override val plugin: Any,
		override val listener: (T) -> Any,
		override val lookup: MethodHandles.Lookup?) : IBouncerScopeListener<T>
}
