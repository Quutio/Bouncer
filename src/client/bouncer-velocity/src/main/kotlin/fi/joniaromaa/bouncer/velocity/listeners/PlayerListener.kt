package fi.joniaromaa.bouncer.velocity.listeners

import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.grpc.ServerFilter
import fi.joniaromaa.bouncer.grpc.ServerFilterGroup
import fi.joniaromaa.bouncer.grpc.ServerFilterType
import fi.joniaromaa.bouncer.grpc.ServerJoinRequest
import fi.joniaromaa.bouncer.grpc.ServerJoinResponse
import fi.joniaromaa.bouncer.grpc.ServerJoinResponse.StatusCase
import fi.joniaromaa.bouncer.grpc.ServerSort
import fi.joniaromaa.bouncer.grpc.ServerSortByPlayerCount
import fi.joniaromaa.bouncer.velocity.VelocityBouncerPlugin
import kotlinx.coroutines.runBlocking

internal class PlayerListener(private val plugin: VelocityBouncerPlugin)
{
	@Subscribe(order = PostOrder.LATE)
	fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent)
	{
		// Don't override
		if (event.initialServer.isPresent)
		{
			return
		}

		val server: RegisteredServer = this.connectToHub(event.player) ?: return

		event.setInitialServer(server)
	}

	@Subscribe(order = PostOrder.LATE)
	fun onKickedFromServer(event: KickedFromServerEvent)
	{
		// Limbo stuff, maybe?
		if (event.kickedDuringServerConnect())
		{
			return
		}

		val server: RegisteredServer = this.connectToHub(event.player) ?: return

		event.result = KickedFromServerEvent.RedirectPlayer.create(server)
	}

	private fun connectToHub(player: Player): RegisteredServer?
	{
		return runBlocking()
		{
			val response: ServerJoinResponse = this@PlayerListener.plugin.stub.join(
				ServerJoinRequest.newBuilder()
				.addFilter(
					ServerFilter.newBuilder()
					.setGroup(
						ServerFilterGroup.newBuilder()
						.setValue("lobby")
					)
				)
				.addFilter(
					ServerFilter.newBuilder()
					.setType(
						ServerFilterType.newBuilder()
						.setValue("hub")
					)
				).addSort(
						ServerSort.newBuilder()
					.setByPlayerCount(
						ServerSortByPlayerCount.newBuilder()
						.setValue(ServerSortByPlayerCount.Order.Ascending)
					)
				).addUser(player.uniqueId.toString())
				.build()
			)

			return@runBlocking when (response.statusCase)
			{
				StatusCase.SUCCESS ->
				{
					val server: BouncerServerInfo =
						this@PlayerListener.plugin.serversById[response.success.serverId] ?: return@runBlocking null

					return@runBlocking plugin.proxy.getServer(server.name).orElse(null)
				}

				else -> null
			}
		}
	}
}
