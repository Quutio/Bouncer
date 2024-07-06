package io.quut.bouncer.common.user

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.quut.bouncer.api.IBouncerScope
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

class UserManager
{
	private val users: ConcurrentHashMap<UUID, UserData> = ConcurrentHashMap()

	private val reservations: Cache<UUID, Reservation> = Caffeine.newBuilder()
		.expireAfterWrite(Duration.ofMinutes(1))
		.build()

	fun createReservation(scope: IBouncerScope, uniqueId: UUID)
	{
		this.reservations.put(uniqueId, Reservation(scope))
	}

	fun createUserData(uniqueId: UUID, fallback: IBouncerScope?): UserData
	{
		val reservation: Reservation? = this.reservations.getIfPresent(uniqueId)

		val userData = UserData(when
		{
			reservation != null && reservation.isValid(Duration.ofSeconds(5)) -> reservation.scope

			else -> fallback
		})

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

	class Reservation(val scope: IBouncerScope)
	{
		private val reservationTime: Long = System.nanoTime()

		fun isValid(timeout: Duration) = System.nanoTime() - this.reservationTime <= timeout.toNanos()
	}

	class UserData(val scope: IBouncerScope?)
}
