package io.quut.bouncer.bukkit

import io.quut.bouncer.common.BouncerAPI
import org.bukkit.Server

internal class BukkitBouncerAPI(private val server: Server, endpoint: String) : BouncerAPI(endpoint)
{
	override fun shutdownSignalHook()
	{
		this.server.shutdown()
	}
}
