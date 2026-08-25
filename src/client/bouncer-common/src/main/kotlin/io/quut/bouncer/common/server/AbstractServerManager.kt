package io.quut.bouncer.common.server

import io.quut.bouncer.api.server.IDistributedServerContainer
import io.quut.bouncer.api.server.IDistributedServerFilter
import io.quut.bouncer.api.server.IDistributedServerManager
import io.quut.bouncer.api.server.IDistributedServerOptions
import io.quut.bouncer.api.server.IDistributedServerWatchRequest
import io.quut.bouncer.api.server.IDistributedServerWatcher
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.grpc.BouncerWatchRequestKt.server
import io.quut.bouncer.grpc.BouncerWatchRequestKt.universe
import io.quut.bouncer.grpc.ServerFilter
import io.quut.bouncer.grpc.ServerFilterKt.group
import io.quut.bouncer.grpc.bouncerWatchRequest
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.time.delay
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractServerManager(
	private val networkManager: NetworkManager,
	private val userManager: UserManager) : IDistributedServerManager
{
	private val startSessionSignal: AtomicReference<CompletableJob> = AtomicReference(Job())
	private var session: ServerManagerSession = ServerManagerSession(this.networkManager, this.userManager)

	private val servers: MutableSet<DistributedServer> = Collections.newSetFromMap(IdentityHashMap())

	init
	{
		@OptIn(DelicateCoroutinesApi::class) // Background task
		GlobalScope.launch()
		{
			while (true)
			{
				try
				{
					this@AbstractServerManager.startSessionSignal.get().join()
					this@AbstractServerManager.session.startAsync()
				}
				catch (e: Throwable)
				{
					e.printStackTrace()
				}
				finally
				{
					this@AbstractServerManager.startSessionSignal.get().join()

					synchronized(this@AbstractServerManager.startSessionSignal)
					{
						this@AbstractServerManager.session.shutdown()
						this@AbstractServerManager.session = ServerManagerSession(this@AbstractServerManager.networkManager, this@AbstractServerManager.userManager)

						this@AbstractServerManager.servers.forEach()
						{ server ->
							server.lostConnection()

							this@AbstractServerManager.session.registerServer(server)
						}
					}
				}

				delay(Duration.ofSeconds(3)) // Wait for 3s before reconnecting
			}
		}
	}

	private fun createServer(options: IDistributedServerOptions): DistributedServer =
		DistributedServer(options.info, options.state)

	override fun registerServer(options: IDistributedServerOptions): IDistributedServerContainer
	{
		val server: DistributedServer = this.createServer(options) // Register the server async

		synchronized(this.startSessionSignal)
		{
			this.servers.add(server)
			this.session.registerServer(server)

			this.startSessionSignal.get().complete()
		}

		// Return the server instance already so it can be mutated
		return DistributedServerContainer(server, this::unregisterServer)
	}

	private fun unregisterServer(server: DistributedServer)
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

			server.unregister()
		}
	}

	override fun watch(request: IDistributedServerWatchRequest): IDistributedServerWatcher
	{
		fun createFilter(filter: IDistributedServerFilter): ServerFilter
		{
			fun unwrap(filter: IDistributedServerFilter, builder: ServerFilter.Builder)
			{
				when (filter)
				{
					is IDistributedServerFilter.IGroup -> builder.group = group { this.value = filter.group }
					is IDistributedServerFilter.INot ->
					{
						builder.inverse = !builder.inverse

						unwrap(filter.filter, builder)
					}
					else -> throw AssertionError()
				}
			}

			val builder = ServerFilter.newBuilder()
			unwrap(filter, builder)

			return builder.build()
		}

		val watcher = ServerWatcher(this.networkManager, request.eventHandler)
		watcher.start(bouncerWatchRequest()
		{
			this.server = server()
			{
				request.filter.forEach { f -> this.filter.add(createFilter(f)) }
			}
			this.universe = universe { }
		})

		return watcher
	}

	internal fun shutdown(intentional: Boolean = false)
	{
		synchronized(this.startSessionSignal)
		{
			this.startSessionSignal.getAndSet(Job())
			this.servers.clear()

			this.session.shutdown(intentional)
		}
	}
}
