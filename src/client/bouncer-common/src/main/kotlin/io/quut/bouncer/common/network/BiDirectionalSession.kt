package io.quut.bouncer.common.network

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.receiveAsFlow
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.atomic.AtomicInteger

internal abstract class BiDirectionalSession<TRequest, TRequestBuilder, TResponse>
{
	private val outgoingChannel: Channel<TRequest> = Channel(capacity = Channel.UNLIMITED)

	private val nextMessageIdRequestId: AtomicInteger = AtomicInteger()
	private val messageCallbacks: ConcurrentMap<Int, CompletableDeferred<*>> = ConcurrentHashMap()

	internal suspend fun startAsync(open: (Flow<TRequest>) -> (Flow<TResponse>))
	{
		open(this.outgoingChannel.receiveAsFlow())
			.cancellable()
			.collect(this::handle)
	}

	protected abstract fun handle(message: TResponse)

	@Suppress("UNCHECKED_CAST")
	protected fun <T> handleCallback(callbackId: Int, message: T)
	{
		val callback: CompletableDeferred<*> = this.messageCallbacks.remove(callbackId) ?: return

		(callback as CompletableDeferred<T>).complete(message)
	}

	internal fun writeAndForget(request: TRequest): Boolean
	{
		return this.outgoingChannel.trySend(request).isSuccess
	}

	protected fun <T> writeAsync(builder: TRequestBuilder): Deferred<T>
	{
		val messageId: Int = this.nextMessageIdRequestId.incrementAndGet()
		val deferred: CompletableDeferred<T> = CompletableDeferred()

		this.messageCallbacks[messageId] = deferred
		this.outgoingChannel.trySend(this.prepareResponse(messageId, builder))

		return deferred
	}

	protected abstract fun prepareResponse(messageId: Int, response: TRequestBuilder): TRequest

	protected fun shutdown()
	{
		this.outgoingChannel.close()
	}
}
