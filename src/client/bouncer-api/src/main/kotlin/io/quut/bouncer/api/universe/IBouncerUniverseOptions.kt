package io.quut.bouncer.api.universe

import io.quut.bouncer.api.IBouncerScopeListener
import java.util.Collections

interface IBouncerUniverseOptions
{
	val info: IBouncerUniverseInfo
	val area: IBouncerUniverseArea
	val listeners: Collection<IBouncerScopeListener<IBouncerUniverse>>
	val stages: List<IBouncerUniverseStageOptions<*>>

	companion object
	{
		@JvmStatic
		fun of(info: IBouncerUniverseInfo, area: IBouncerUniverseArea, listeners: Collection<IBouncerScopeListener<IBouncerUniverse>>, stages: List<IBouncerUniverseStageOptions<*>>): IBouncerUniverseOptions
		{
			return Impl(info, area, Collections.unmodifiableCollection(listeners), Collections.unmodifiableList(stages))
		}
	}

	private class Impl(
		override var info: IBouncerUniverseInfo,
		override val area: IBouncerUniverseArea,
		override val listeners: Collection<IBouncerScopeListener<IBouncerUniverse>>,
		override val stages: List<IBouncerUniverseStageOptions<*>>) : IBouncerUniverseOptions
}
