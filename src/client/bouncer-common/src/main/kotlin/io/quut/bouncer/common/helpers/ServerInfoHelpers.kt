package io.quut.bouncer.common.helpers

import java.net.DatagramSocket
import java.net.InetAddress

object ServerInfoHelpers
{
	fun resolveHostAddress(bindAddress: String): String
	{
		val address: String = System.getenv("SERVER_IP") ?: bindAddress
		if (address.isNotEmpty())
		{
			return address
		}

		DatagramSocket().use()
		{ socket ->
			socket.connect(InetAddress.getByName("1.1.1.1"), 53)

			return socket.localAddress.hostAddress
		}
	}
}
