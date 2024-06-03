package io.quut.bouncer.common.server

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IBouncerServer
import io.quut.bouncer.api.server.IServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.harmony.api.IHarmonyEventManager
import io.quut.harmony.api.IHarmonyScopeOptions
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

abstract class AbstractServerManager(private val stub: BouncerGrpcKt.BouncerCoroutineStub) : IServerManager
{
	private val startSessionSignal: AtomicReference<CompletableJob> = AtomicReference(Job())
	private var session: ServerManagerSession = ServerManagerSession(this, this.stub)

	private val servers: MutableSet<AbstractBouncerServer> = Collections.newSetFromMap(IdentityHashMap())

	private var eventManager: IHarmonyEventManager<IBouncerScope>? = null

	override var defaultServer: IBouncerServer? = null

	@JvmField
	protected var fallback: IBouncerScope? = null

	private val reservations: Cache<UUID, Reservation> = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofMinutes(1))
		.build()

	private val users: ConcurrentHashMap<UUID, UserData> = ConcurrentHashMap()

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
						this@AbstractServerManager.session = ServerManagerSession(this@AbstractServerManager, this@AbstractServerManager.stub)

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

	protected fun init()
	{
		this.eventManager = this.createEventManager()
	}

	protected abstract fun createEventManager(): IHarmonyEventManager<IBouncerScope>?
	protected abstract fun createServer(info: BouncerServerInfo, eventManager: IHarmonyEventManager<IBouncerScope>?): AbstractBouncerServer

	override fun registerServer(info: BouncerServerInfo, harmony: IHarmonyScopeOptions<IBouncerServer>): IBouncerServer
	{
		val server: AbstractBouncerServer = this.createServer(info, this.eventManager) // Register the server async

		synchronized(this.startSessionSignal)
		{
			this.eventManager?.registerScope(server, harmony)

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

	internal fun createReservation(scope: IBouncerScope, uniqueId: UUID)
	{
		this.reservations.put(uniqueId, Reservation(scope))
	}

	protected fun createUserData(uniqueId: UUID): UserData
	{
		val reservation: Reservation? = this.reservations.getIfPresent(uniqueId)

		val userData = UserData(when
		{
			reservation != null && reservation.isValid(Duration.ofSeconds(5)) -> reservation.scope

			else -> this.fallback
		})

		this.users[uniqueId] = userData

		return userData
	}

	protected fun getUser(uniqueId: UUID): UserData?
	{
		return this.users[uniqueId]
	}

	protected fun userDisconnected(uniqueId: UUID): UserData?
	{
		return this.users.remove(uniqueId)
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

	protected class Reservation(val scope: IBouncerScope)
	{
		private val reservationTime: Long = System.nanoTime()

		fun isValid(timeout: Duration) = System.nanoTime() - this.reservationTime <= timeout.toNanos()
	}

	protected class UserData(val scope: IBouncerScope?)
}
