package io.quut.bouncer.api.game

import io.quut.bouncer.api.IBouncerScopeListener
import java.util.Collections

interface IBouncerGameOptions
{
	val info: IBouncerGameInfo
	val area: IBouncerGameArea
	val listeners: Collection<IBouncerScopeListener<IBouncerGame>>
	val stages: Collection<IBouncerGameStageType<*>>

	companion object
	{
		@JvmStatic
		fun of(info: IBouncerGameInfo, area: IBouncerGameArea, listeners: Collection<IBouncerScopeListener<IBouncerGame>>, stages: Collection<IBouncerGameStageType<*>>): IBouncerGameOptions
		{
			return Impl(info, area, Collections.unmodifiableCollection(listeners), Collections.unmodifiableCollection(stages))
		}
	}

	private class Impl(
		override var info: IBouncerGameInfo,
		override val area: IBouncerGameArea,
		override val listeners: Collection<IBouncerScopeListener<IBouncerGame>>,
		override val stages: Collection<IBouncerGameStageType<*>>) : IBouncerGameOptions
}
