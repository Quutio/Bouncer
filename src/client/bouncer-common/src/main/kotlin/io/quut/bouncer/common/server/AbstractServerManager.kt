package io.quut.bouncer.common.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.game.IBouncerGame
import io.quut.bouncer.api.game.IBouncerGameArea
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.api.server.IServerManager
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.common.user.UserManager
import io.quut.harmony.api.IHarmonyEventListener
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyScopeOptions
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

abstract class AbstractServerManager(private val networkManager: NetworkManager, val userManager: UserManager) : IServerManager
{
	private val startSessionSignal: AtomicReference<CompletableJob> = AtomicReference(Job())
	private var session: ServerManagerSession = ServerManagerSession(this, this.networkManager)

	private val servers: MutableSet<AbstractBouncerServer> = Collections.newSetFromMap(IdentityHashMap())

	protected val worlds: MutableMap<Key, IBouncerScope> = hashMapOf()

	private var eventManager: IHarmonyEventManager<IBouncerScope>? = null

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

	internal fun register(game: IBouncerGame, area: IBouncerGameArea)
	{
		if (area is IBouncerGameArea.IWorld)
		{
			this.worlds[area.worldKey] = game
		}
		else if (area is IBouncerGameArea.ICompound)
		{
			area.scopes.forEach { area -> this.register(game, area) }
		}
	}

	protected fun init()
	{
		this.eventManager = this.createEventManager()
	}

	protected abstract fun createEventManager(): IHarmonyEventManager<IBouncerScope>?
	protected abstract fun createServer(options: IBouncerServerOptions, eventManager: IHarmonyEventManager<IBouncerScope>?): AbstractBouncerServer

	override fun registerServer(options: IBouncerServerOptions): IBouncerServer
	{
		val server: AbstractBouncerServer = this.createServer(options, this.eventManager) // Register the server async

		synchronized(this.startSessionSignal)
		{
			this.eventManager?.registerScope(server, IHarmonyScopeOptions.of(
				listeners = options.listeners.map { listener -> IHarmonyEventListener.of(listener.plugin, listener.listener, listener.lookup) }.toTypedArray()))

			this.servers.add(server)
			this.session.registerServer(server)

			this.startSessionSignal.get().complete()
		}

		// Return the server instance already so it can be mutated
		return server
	}

	override fun unregisterServer(server: IBouncerServer)
	{
		this.unregisterServer(server as AbstractBouncerServer)
	}

	private fun unregisterServer(server: AbstractBouncerServer)
	{
		synchronized(this.startSessionSignal)
		{
			if (!this.servers.remove(server))
			{
				return
			}

			this.eventManager?.unregisterScope(server)

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
