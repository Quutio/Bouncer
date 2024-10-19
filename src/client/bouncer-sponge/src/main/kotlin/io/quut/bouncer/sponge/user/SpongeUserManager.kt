package io.quut.bouncer.sponge.user

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.common.user.UserManager
import org.spongepowered.api.network.ServerSideConnection
import java.time.Duration
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class SpongeUserManager : UserManager()
{
	private val connections: ConcurrentMap<ServerSideConnection, UserData> = ConcurrentHashMap()

	internal fun establishConnection(connection: ServerSideConnection, fallback: IBouncerScope?, reservationId: Int? = null)
	{
		val reservation: Reservation? = reservationId?.let(this.reservations::getIfPresent)

		val userData = UserData(when
		{
			reservation != null && reservation.isValid(Duration.ofSeconds(5)) -> reservation.scope

			else -> fallback
		})

		this.connections[connection] = userData
	}

	internal fun getUser(connection: ServerSideConnection): UserData = this.connections[connection]!!

	internal fun userDisconnected(connection: ServerSideConnection, uniqueId: UUID?)
	{
		val userData: UserData = this.connections.remove(connection) ?: return

		this.users.remove(uniqueId, userData)
	}
}
