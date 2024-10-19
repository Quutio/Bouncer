package io.quut.bouncer.common.user

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.quut.bouncer.api.IBouncerScope
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

open class UserManager
{
	protected val users: ConcurrentHashMap<UUID, UserData> = ConcurrentHashMap()

	protected val reservations: Cache<Int, Reservation> = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofMinutes(1))
		.build()

	fun createReservation(reservationId: Int, scope: IBouncerScope, players: Set<UUID>)
	{
		this.reservations.put(reservationId, Reservation(scope, players))
	}

	fun createUserData(uniqueId: UUID, userData: UserData): UserData
	{
		this.users[uniqueId] = userData

		return userData
	}

	fun getUser(uniqueId: UUID): UserData?
	{
		return this.users[uniqueId]
	}

	fun userDisconnected(uniqueId: UUID): UserData?
	{
		return this.users.remove(uniqueId)
	}

	class Reservation(val scope: IBouncerScope, val players: Set<UUID>)
	{
		private val reservationTime: Long = System.nanoTime()

		fun isValid(timeout: Duration) = System.nanoTime() - this.reservationTime <= timeout.toNanos()
	}

	class UserData(val scope: IBouncerScope?)
}
