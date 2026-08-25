package io.quut.bouncer.velocity.universe

import com.google.inject.Inject
import com.velocitypowered.api.proxy.ProxyServer
import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisor
import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisorProvider
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.universe.DistributedUniverseManager
import io.quut.bouncer.grpc.JoinUniverseResponse
import io.quut.bouncer.velocity.server.DynamicServerEventHandler
import io.quut.bouncer.velocity.utils.Const
import io.quut.fusion.api.connection.IConnectionRequestTemplate
import io.quut.fusion.velocity.api.IVelocityFusionAPI
import net.kyori.adventure.key.Key
import java.nio.ByteBuffer
import java.util.ServiceLoader

internal class VelocityDistributedUniverseManager @Inject constructor(
	private val server: ProxyServer,
	networkManager: NetworkManager,
	private val servers: DynamicServerEventHandler) : DistributedUniverseManager(networkManager)
{
	private val supervisors: Map<Key, IDistributedUniverseSupervisor> = this.resolveSupervisors()

	override fun join(request: IDistributedUniverseJoinRequest, response: JoinUniverseResponse): Boolean
	{
		this.servers.getServer(response.success.serverId)?.let()
		{ server ->
			this.servers.getUniverse(response.success.universeId)?.let()
			{ (_, info) ->
				val supervisor: IDistributedUniverseSupervisor = this@VelocityDistributedUniverseManager.supervisors[info.type] ?: return false

				val template: IConnectionRequestTemplate = IVelocityFusionAPI.get().fusion.connectionRequestTemplate(server)
				{ request ->
					request.loginPluginMessageHandler(Const.HANDSHAKE_CHANNEL_KEY)
						{ _ -> ByteBuffer.allocate(4).putInt(response.success.reservationId).array() }
				}

				request.players.map(this.server::getPlayer).forEach()
				{ player ->
					if (player.isPresent)
					{
						supervisor.join(player.get(), template, info)
					}
				}

				return true
			}
		}

		return false
	}

	private fun resolveSupervisors(): Map<Key, IDistributedUniverseSupervisor>
	{
		val map: MutableMap<Key, IDistributedUniverseSupervisor> = hashMapOf()

		ServiceLoader.load(IDistributedUniverseSupervisorProvider::class.java).forEach { e -> e.provide(map::put) }

		return map.toMap()
	}
}
