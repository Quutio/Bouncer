package io.quut.bouncer.sponge

import io.quut.bouncer.common.BouncerAPI
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.sponge.server.SpongeServerManager
import org.spongepowered.api.Game
import org.spongepowered.plugin.PluginContainer

internal class SpongeBouncerAPI(private val pluginContainer: PluginContainer, private val game: Game, endpoint: String) : BouncerAPI(endpoint)
{
	init
	{
		this.init()
	}

	override fun createServerManager(stub: BouncerGrpcKt.BouncerCoroutineStub): AbstractServerManager
	{
		return SpongeServerManager(this.pluginContainer, stub)
	}

	override fun onShutdownSignal()
	{
		if (this.game.isServerAvailable)
		{
			this.game.server().shutdown()
		}
	}
}
