package io.quut.bouncer.velocity.listeners

import com.google.common.cache.Cache
import com.google.common.cache.CacheBuilder
import com.google.protobuf.ByteString
import com.velocitypowered.api.event.PostOrder
import com.velocitypowered.api.event.Subscribe
import com.velocitypowered.api.event.connection.DisconnectEvent
import com.velocitypowered.api.event.connection.PostLoginEvent
import com.velocitypowered.api.event.player.KickedFromServerEvent
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent
import com.velocitypowered.api.event.player.ServerConnectedEvent
import com.velocitypowered.api.proxy.Player
import com.velocitypowered.api.proxy.server.RegisteredServer
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.grpc.ServerFilterKt.group
import io.quut.bouncer.grpc.ServerFilterKt.name
import io.quut.bouncer.grpc.ServerFilterKt.type
import io.quut.bouncer.grpc.ServerJoinRequest
import io.quut.bouncer.grpc.ServerJoinResponse
import io.quut.bouncer.grpc.ServerJoinResponse.StatusCase
import io.quut.bouncer.grpc.ServerSort.ByPlayerCount
import io.quut.bouncer.grpc.ServerSortKt.byPlayerCount
import io.quut.bouncer.grpc.serverFilter
import io.quut.bouncer.grpc.serverSort
import io.quut.bouncer.velocity.VelocityBouncerPlugin
import io.quut.bouncer.velocity.extensions.eventTask
import io.quut.bouncer.velocity.extensions.toByteArray
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class PlayerListener(private val plugin: VelocityBouncerPlugin)
{
	private val cache: Cache<UUID, ConnectionFailure> = CacheBuilder.newBuilder()
		.expireAfterWrite(5, TimeUnit.MINUTES)
		.build()

	private val retryAttemptCounts: Int = 3
	private val retryDuration: Duration = Duration.ofSeconds(5)

	@Subscribe(order = PostOrder.LATE)
	fun onPostJoin(event: PostLoginEvent)
	{
		this.cache.invalidate(event.player.uniqueId)
	}

	@Subscribe(order = PostOrder.LATE)
	fun onPlayerChooseInitialServer(event: PlayerChooseInitialServerEvent) = eventTask()
	{
		val player: Player = event.player

		for (i in 0..this.retryAttemptCounts)
		{
			if (!player.isActive)
			{
				break
			}

			val server: RegisteredServer? = this.connectToHub(player)
			if (server == null)
			{
				delay(this.retryDuration)
				continue
			}

			event.setInitialServer(server)

			break
		}
	}

	@Subscribe(order = PostOrder.LATE)
	fun onKickedFromServer(event: KickedFromServerEvent) = eventTask()
	{
		val player: Player = event.player

		if (event.kickedDuringServerConnect())
		{
			return@eventTask
		}

		val failure: ConnectionFailure = this.cache.asMap().computeIfAbsent(player.uniqueId) { _ -> ConnectionFailure() }
		failure.add(event.server)

		var server: RegisteredServer? = this.connectToHub(player, failure)
		while (server == null)
		{
			if (failure.nextAttempt() > this.retryAttemptCounts)
			{
				return@eventTask
			}

			delay(this.retryDuration)

			if (!player.isActive)
			{
				return@eventTask
			}

			server = this.connectToHub(player, failure) ?: continue
		}

		event.result = KickedFromServerEvent.RedirectPlayer.create(server)
	}

	@Subscribe(order = PostOrder.LATE)
	fun onServerConnectedEvent(event: ServerConnectedEvent)
	{
		this.cache.invalidate(event.player.uniqueId)
	}

	@Subscribe(order = PostOrder.LATE)
	fun onDisconnect(event: DisconnectEvent)
	{
		when (event.loginStatus)
		{
			DisconnectEvent.LoginStatus.SUCCESSFUL_LOGIN,
			DisconnectEvent.LoginStatus.CONFLICTING_LOGIN,
			DisconnectEvent.LoginStatus.PRE_SERVER_JOIN -> this.cache.invalidate(event.player.uniqueId)

			else -> Unit
		}
	}

	private suspend fun connectToHub(player: Player, failure: ConnectionFailure? = null): RegisteredServer?
	{
		val builder: ServerJoinRequest.Builder = ServerJoinRequest.newBuilder()
			.addFilter(
				serverFilter()
				{
					this.group = group()
					{
						this.value = "lobby"
					}
				}
			).addFilter(
				serverFilter()
				{
					this.type = type()
					{
						this.value = "hub"
					}
				}
			).addSort(
				serverSort()
				{
					this.byPlayerCount = byPlayerCount()
					{
						this.value = ByPlayerCount.Order.ASCENDING
					}
				}
			).addPlayers(ByteString.copyFrom(player.uniqueId.toByteArray()))

		failure?.servers?.forEach()
		{ server ->
			builder.addFilter(
				serverFilter()
				{
					this.inverse = true
					this.name = name()
					{
						this.value = server
					}
				}
			)
		}

		val response: ServerJoinResponse = this.plugin.stub.joinServer(builder.build())
		when (response.statusCase)
		{
			StatusCase.SUCCESS ->
			{
				val server: BouncerServerInfo =
					this@PlayerListener.plugin.serversById[response.success.serverId] ?: return null

				return plugin.proxy.getServer(server.name).orElse(null)
			}

			else -> return null
		}
	}

	private class ConnectionFailure
	{
		private val _servers: MutableSet<String> = mutableSetOf()

		private var attemptCount: Int = 0

		val servers: Set<String>
			get() = this._servers

		fun add(server: RegisteredServer)
		{
			this._servers.add(server.serverInfo.name)
		}

		fun nextAttempt(): Int
		{
			this._servers.clear()

			return ++this.attemptCount
		}
	}
}
