package io.quut.bouncer.velocity.extensions

import com.velocitypowered.api.event.EventTask
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

fun eventTask(fn: suspend () -> Unit): EventTask
{
	return EventTask.withContinuation()
	{ continuation ->
		val completion = object : Continuation<Unit>
		{
			override val context: CoroutineContext
				get() = EmptyCoroutineContext

			override fun resumeWith(result: Result<Unit>)
			{
				if (result.isFailure)
				{
					continuation.resumeWithException(result.exceptionOrNull())
				}
				else
				{
					continuation.resume()
				}
			}
		}

		fn.startCoroutine(completion)
	}
}
