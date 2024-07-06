package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameOptions
import java.util.UUID

interface IBouncerServer : IBouncerScope
{
	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun heartbeat(tps: Int? = null, memory: Int? = null)

	fun registerGame(options: IBouncerGameOptions): IBouncerGame
	fun unregisterGame(game: IBouncerGame)
}
