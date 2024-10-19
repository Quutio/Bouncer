package io.quut.bouncer.common.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerFilter
import io.quut.bouncer.api.server.IBouncerServerManager
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.api.server.IBouncerServerWatchRequest
import io.quut.bouncer.api.server.IBouncerServerWatcher
import io.quut.bouncer.api.universe.IBouncerUniverse
import io.quut.bouncer.api.universe.IBouncerUniverseArea
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.universe.BouncerUniverse
import io.quut.bouncer.common.user.UserManager
import io.quut.bouncer.grpc.BouncerWatchRequestKt.server
import io.quut.bouncer.grpc.ServerFilter
import io.quut.bouncer.grpc.ServerFilterKt.group
import io.quut.bouncer.grpc.bouncerWatchRequest
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.key.Key
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractServerManager<TServer, TUniverse>(private val networkManager: NetworkManager, val userManager: UserManager) : IBouncerServerManager
	where TServer : BouncerServer<TServer, TUniverse>, TUniverse : BouncerUniverse<TServer, TUniverse>
{
	private val startSessionSignal: AtomicReference<CompletableJob> = AtomicReference(Job())
	private var session: ServerManagerSession = ServerManagerSession(this, this.networkManager)

	private val servers: MutableSet<TServer> = Collections.newSetFromMap(IdentityHashMap())

	protected val worlds: MutableMap<Key, IBouncerScope> = hashMapOf()

	override var defaultServer: IBouncerServer? = null

	@JvmField
	var fallback: IBouncerScope? = null

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
						this@AbstractServerManager.session = ServerManagerSession(this@AbstractServerManager, this@AbstractServerManager.networkManager)

						this@AbstractServerManager.servers.forEach()
						{ server ->
							server.lostConnection()

							this@AbstractServerManager.session.registerServer(server)
						}
					}
				}

				delay(TimeUnit.SECONDS.toMillis(3)) // Wait for 3s before reconnecting
			}
		}
	}

	internal open fun init()
	{
	}

	internal fun register(universe: IBouncerUniverse, area: IBouncerUniverseArea)
	{
		if (area is IBouncerUniverseArea.IWorld)
		{
			this.worlds[area.worldKey] = universe
		}
		else if (area is IBouncerUniverseArea.ICompound)
		{
			area.scopes.forEach { area -> this.register(universe, area) }
		}
	}

	protected abstract fun createServer(options: IBouncerServerOptions): TServer

	override fun registerServer(options: IBouncerServerOptions): IBouncerServer
	{
		val server: TServer = this.createServer(options) // Register the server async

		synchronized(this.startSessionSignal)
		{
			this.registerServer(options, server)

			this.servers.add(server)
			this.session.registerServer(server)

			this.startSessionSignal.get().complete()
		}

		// Return the server instance already so it can be mutated
		return server
	}

	protected open fun registerServer(options: IBouncerServerOptions, server: TServer)
	{
	}

	override fun unregisterServer(server: IBouncerServer)
	{
		this.unregisterServer(server as BouncerServer<*, *>)
	}

	private fun unregisterServer(server: BouncerServer<*, *>)
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

	override fun setFallback(scope: IBouncerScope)
	{
		this.fallback = scope
	}

	override fun watch(request: IBouncerServerWatchRequest): IBouncerServerWatcher
	{
		fun createFilter(filter: IBouncerServerFilter): ServerFilter
		{
			fun unwrap(filter: IBouncerServerFilter, builder: ServerFilter.Builder)
			{
				when (filter)
				{
					is IBouncerServerFilter.IGroup -> builder.group = group { this.value = filter.group }
					is IBouncerServerFilter.INot ->
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
		watcher.start(
			bouncerWatchRequest()
			{
				this.server = server {
					request.filter.forEach { f -> this.filter.add(createFilter(f)) }
				}
			})

		return watcher
	}

	internal fun shutdown(intentional: Boolean = false)
	{
		synchronized(this.startSessionSignal)
		{
			this.fallback = null
			this.defaultServer = null

			this.startSessionSignal.getAndSet(Job())
			this.servers.clear()

			this.session.shutdown(intentional)
		}
	}
}
