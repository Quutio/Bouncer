package io.quut.bouncer.api.game

import io.quut.bouncer.api.IBouncerScope

interface IBouncerGame : IBouncerScope
{
	fun <T : Any> switchStage(type: IBouncerGameStageType<T>)
	fun nextStage()
}
