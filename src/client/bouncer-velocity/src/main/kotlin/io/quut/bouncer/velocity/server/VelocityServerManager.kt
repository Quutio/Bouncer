package io.quut.bouncer.velocity.server

import io.quut.bouncer.api.IBouncerScope
import io.quut.bouncer.api.server.IBouncerServerOptions
import io.quut.bouncer.common.server.AbstractBouncerServer
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.harmony.api.IHarmonyEventManager

class VelocityServerManager(stub: BouncerGrpcKt.BouncerCoroutineStub) : AbstractServerManager(stub)
{
	override fun createEventManager(): IHarmonyEventManager<IBouncerScope>?
	{
		return null
	}

	override fun createServer(info: IBouncerServerOptions, eventManager: IHarmonyEventManager<IBouncerScope>?): AbstractBouncerServer
	{
		TODO("Not yet implemented")
	}
}
