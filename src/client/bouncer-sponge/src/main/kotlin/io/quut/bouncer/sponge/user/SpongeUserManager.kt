package io.quut.bouncer.sponge.user

import io.quut.bouncer.common.user.UserManager
import org.spongepowered.api.network.ServerSideConnection
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap

internal class SpongeUserManager : UserManager()
{
	private val connections: ConcurrentMap<ServerSideConnection, UserData> = ConcurrentHashMap()

	internal fun establishConnection(connection: ServerSideConnection, reservationId: Int? = null)
	{
		val reservation: Reservation? = reservationId?.let(this.reservations::getIfPresent)

		this.connections[connection] = UserData()
	}

	internal fun getUser(connection: ServerSideConnection): UserData = this.connections[connection]!!

	internal fun userDisconnected(connection: ServerSideConnection, uniqueId: UUID?)
	{
		val userData: UserData = this.connections.remove(connection) ?: return

		this.users.remove(uniqueId, userData)
	}
}
