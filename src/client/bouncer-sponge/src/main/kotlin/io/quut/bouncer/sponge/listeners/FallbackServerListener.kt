package io.quut.bouncer.sponge.listeners

import io.quut.bouncer.api.server.IBouncerServer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.network.ServerSideConnectionEvent

internal object FallbackServerListener
{
	internal class Accept(private val bouncerServer: IBouncerServer)
	{
		@Listener(order = Order.POST)
		private fun onJoin(event: ServerSideConnectionEvent.Join)
		{
			this.bouncerServer.confirmJoin(event.player().uniqueId())
		}

		@Listener(order = Order.POST)
		private fun onLeave(event: ServerSideConnectionEvent.Leave)
		{
			this.bouncerServer.confirmLeave(event.player().uniqueId())
		}
	}
}
