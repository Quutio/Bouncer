package io.quut.bouncer.velocity.queue

import com.google.protobuf.ByteString
import com.velocitypowered.api.proxy.ProxyServer
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.GameQueueRequest
import io.quut.bouncer.grpc.GameQueueRequestKt
import io.quut.bouncer.grpc.GameQueueResponse
import io.quut.bouncer.grpc.gameQueueRequest
import io.quut.bouncer.velocity.extensions.toByteArray
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.format.NamedTextColor
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

class QueueManager(private val stub: BouncerGrpcKt.BouncerCoroutineStub, private val proxyServer: ProxyServer)
{
	private val requestChannel: Channel<GameQueueRequest> = Channel(capacity = Channel.UNLIMITED)

	private val nextRequestId: AtomicInteger = AtomicInteger()
	private val requestResponses: ConcurrentMap<Int, CompletableDeferred<*>> = ConcurrentHashMap()

	private val nextTrackingId: AtomicInteger = AtomicInteger()
	private val queuesByTrackingId: MutableMap<Int, BouncerQueue> = ConcurrentHashMap()
	private val queuesById: MutableMap<Int, BouncerQueue> = ConcurrentHashMap()
	private val queuesByUser: MutableMap<UUID, BouncerQueue> = ConcurrentHashMap()

	init
	{
		GlobalScope.launch()
		{
			while (true)
			{
				kotlin.runCatching {
					this@QueueManager.stub.gameQueue(this@QueueManager.requestChannel.receiveAsFlow())
						.cancellable()
						.collect(this@QueueManager::handleResponse)
				}
			}
		}
	}

	@Suppress("UNCHECKED_CAST")
	private fun handleResponse(response: GameQueueResponse)
	{
		when (response.messageCase)
		{
			GameQueueResponse.MessageCase.PREPARE -> return this.handlePrepare(response.requestId, response.prepare)
			GameQueueResponse.MessageCase.CONFIRMATION -> return this.handleConfirmation(response.confirmation)

			else -> Unit
		}

		val callback: CompletableDeferred<*> = this.requestResponses.remove(response.requestId) ?: return
		when (response.messageCase)
		{
			GameQueueResponse.MessageCase.CONFIRMJOIN ->
				(callback as CompletableDeferred<GameQueueResponse.ConfirmJoin>).complete(response.confirmJoin)

			else -> Unit
		}
	}

	private fun handlePrepare(requestId: Int, message: GameQueueResponse.Prepapre)
	{
		message.queueIdsList.forEach()
		{ id ->
			val queue: BouncerQueue = this.queuesById[id] ?: return@forEach
			queue.users.forEach { u ->
				proxyServer.getPlayer(u).ifPresent { player ->
					player.sendMessage(Component.text("Confirming match..", NamedTextColor.GRAY))
				}
			}
		}

		Thread.sleep(3000)

		this.requestChannel.trySend(
			gameQueueRequest()
			{
				this.requestId = requestId
				this.confirmPrepare = GameQueueRequestKt.confirmPrepapre()
				{
				}
			}
		)
	}

	private fun handleConfirmation(message: GameQueueResponse.Confirmation)
	{
		message.queueIdsList.forEach()
		{ id ->
			val queue: BouncerQueue = this.queuesById[id] ?: return@forEach
			queue.users.forEach { u ->
				proxyServer.getPlayer(u).ifPresent { player ->
					player.sendMessage(
						Component.text("Match found! ", NamedTextColor.GRAY)
							.append(Component.text("Accept", NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("queue accept")))
					)
				}
			}
		}
	}

	@OptIn(ExperimentalCoroutinesApi::class)
	fun join(user: UUID, gamemode: String)
	{
		val trackingId: Int = this.nextTrackingId.incrementAndGet()
		val queue = BouncerQueue(setOf(user))

		this.queuesByTrackingId[trackingId] = queue
		this.queuesByUser[user] = queue

		val job: Deferred<GameQueueResponse.ConfirmJoin> = this.sendRequestAsync(
			GameQueueRequest.newBuilder()
				.setJoin(
					GameQueueRequestKt.join()
					{
						this.trackingId = trackingId
						this.gamemode = gamemode
						this.players.add(ByteString.copyFrom(user.toByteArray()))
					}
				)
		)

		job.invokeOnCompletion()
		{ ex ->
			this.queuesById[job.getCompleted().success.queueId] = queue
		}
	}

	private fun <T> sendRequestAsync(builder: GameQueueRequest.Builder): Deferred<T>
	{
		val requestId: Int = this.nextRequestId.incrementAndGet()
		val deferred: CompletableDeferred<T> = CompletableDeferred()

		this.requestResponses[requestId] = deferred
		this.requestChannel.trySend(builder.setRequestId(requestId).build())

		return deferred
	}
}
