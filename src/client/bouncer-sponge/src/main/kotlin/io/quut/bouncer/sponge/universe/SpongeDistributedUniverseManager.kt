package io.quut.bouncer.sponge.universe

import com.google.inject.Inject
import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.universe.DistributedUniverseManager
import io.quut.bouncer.grpc.JoinUniverseResponse
import org.spongepowered.api.Game
import org.spongepowered.api.entity.living.player.server.ServerPlayer
import org.spongepowered.api.network.channel.raw.RawDataChannel
import kotlin.jvm.optionals.getOrNull

internal class SpongeDistributedUniverseManager @Inject constructor(
	private val game: Game,
	networkManager: NetworkManager) : DistributedUniverseManager(networkManager)
{
	internal lateinit var playChannel: RawDataChannel

	override fun join(request: IDistributedUniverseJoinRequest, response: JoinUniverseResponse): Boolean
	{
		val player: ServerPlayer = request.players.firstNotNullOf { player -> this.game.server().player(player).getOrNull() } ?: return false

		this.playChannel.play().sendTo(player)
		{ buffer ->
			buffer.writeUTF(request.id.asString())
			buffer.writeInt(request.players.size)

			request.players.forEach()
			{ player ->
				buffer.writeLong(player.mostSignificantBits)
				buffer.writeLong(player.leastSignificantBits)
			}
		}

		return true
	}
}
