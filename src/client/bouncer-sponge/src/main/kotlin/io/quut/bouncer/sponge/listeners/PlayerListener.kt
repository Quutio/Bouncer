package io.quut.bouncer.sponge.listeners

import io.quut.bouncer.api.server.IBouncerServer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.network.ServerSideConnectionEvent

internal class PlayerListener(private val bouncerServer: IBouncerServer)
{
	@Listener
	private fun onJoin(event: ServerSideConnectionEvent.Join)
	{
		this.bouncerServer.confirmJoin(event.player().uniqueId())
	}

	@Listener
	private fun onLeave(event: ServerSideConnectionEvent.Disconnect)
	{
		this.bouncerServer.confirmLeave(event.player().uniqueId())
	}
}
