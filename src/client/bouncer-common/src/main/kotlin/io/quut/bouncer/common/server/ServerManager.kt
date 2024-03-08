package io.quut.bouncer.common.server

import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IServerManager
import io.quut.bouncer.grpc.ServerServiceGrpcKt
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

internal class ServerManager(private val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub) :
	IServerManager
{
	private val startSessionSignal: AtomicReference<CompletableJob> = AtomicReference(Job())
	private var session: ServerManagerSession = ServerManagerSession(this.stub)

	private val servers: MutableSet<BouncerServer> = Collections.newSetFromMap(IdentityHashMap())

	init
	{
		@OptIn(DelicateCoroutinesApi::class) // Background task
		GlobalScope.launch()
		{
			while (true)
			{
				try
				{
					this@ServerManager.startSessionSignal.get().join()
					this@ServerManager.session.start()
				}
				catch (e: Throwable)
				{
					e.printStackTrace()
				}
				finally
				{
					this@ServerManager.startSessionSignal.get().join()

					synchronized(this@ServerManager.startSessionSignal)
					{
						this@ServerManager.session = ServerManagerSession(this@ServerManager.stub)

						this@ServerManager.servers.forEach()
						{ server ->
							server.lostConnection()

							this@ServerManager.session.registerServer(server)
						}
					}
				}

				delay(TimeUnit.SECONDS.toMillis(3)) // Wait for 3s before reconnecting
			}
		}
	}

	override fun registerServer(info: BouncerServerInfo): IBouncerServer
	{
		val server = BouncerServer(info) // Register the server async

		synchronized(this.startSessionSignal)
		{
			this.servers.add(server)
			this.session.registerServer(server)

			this.startSessionSignal.get().complete()
		}

		// Return the server instance already so it can be mutated
		return server
	}

	override fun unregisterServer(server: IBouncerServer)
	{
		this.unregisterServer(server as BouncerServer)
	}

	private fun unregisterServer(server: BouncerServer)
	{
		synchronized(this.startSessionSignal)
		{
			if (!this.servers.remove(server))
			{
				return
			}

			if (this.servers.isEmpty())
			{
				this.startSessionSignal.getAndSet(Job())
			}

			this.session.unregisterServer(server)
		}
	}

	internal fun shutdown()
	{
		synchronized(this.startSessionSignal)
		{
			this.startSessionSignal.getAndSet(Job())
			this.servers.clear()

			this.session.shutdown()
		}
	}
}
