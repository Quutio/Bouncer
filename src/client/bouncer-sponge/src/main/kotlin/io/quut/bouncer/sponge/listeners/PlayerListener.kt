package io.quut.bouncer.sponge.listeners

import io.quut.bouncer.api.server.IBouncerServer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.network.ServerSideConnectionEvent

class PlayerListener(private val bouncerServer: IBouncerServer)
{
	@Listener
	fun onJoin(event: ServerSideConnectionEvent.Join)
	{
		this.bouncerServer.confirmJoin(event.player().uniqueId())
	}

	@Listener
	fun onLeave(event: ServerSideConnectionEvent.Disconnect)
	{
		this.bouncerServer.confirmLeave(event.player().uniqueId())
	}
}
