package io.quut.bouncer.api.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameOptions
import io.quut.harmony.api.IHarmonyScopeOptions
import java.util.UUID

interface IBouncerServer : IBouncerScope
{
	fun confirmJoin(uniqueId: UUID)
	fun confirmLeave(uniqueId: UUID)

	fun heartbeat(tps: Int? = null, memory: Int? = null)

	fun registerGame(options: IBouncerGameOptions, harmony: IHarmonyScopeOptions<IBouncerGame>): IBouncerGame
	fun unregisterGame(game: IBouncerGame)
}
