package io.quut.bouncer.bukkit

import io.quut.bouncer.bukkit.server.BukkitServerManager
import io.quut.bouncer.common.BouncerAPI
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import org.bukkit.Server

internal class BukkitBouncerAPI(private val server: Server, endpoint: String) : BouncerAPI(endpoint)
{
	init
	{
		this.init()
	}

	override fun createServerManager(stub: BouncerGrpcKt.BouncerCoroutineStub): AbstractServerManager
	{
		return BukkitServerManager(stub)
	}

	override fun onShutdownSignal()
	{
		this.server.shutdown()
	}
}
