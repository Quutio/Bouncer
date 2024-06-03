package io.quut.bouncer.bukkit.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.harmony.api.IHarmonyEventManager

internal class BukkitServerManager(stub: BouncerGrpcKt.BouncerCoroutineStub) : AbstractServerManager(stub)
{
	override fun createEventManager(): IHarmonyEventManager<IBouncerScope>?
	{
		return null
	}

	override fun createServer(info: BouncerServerInfo, eventManager: IHarmonyEventManager<IBouncerScope>?): AbstractBouncerServer
	{
		return BukkitBouncerServer(eventManager, info)
	}
}
