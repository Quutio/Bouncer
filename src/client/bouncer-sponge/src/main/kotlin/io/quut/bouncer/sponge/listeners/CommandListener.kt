package io.quut.bouncer.sponge.listeners

import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.command.ExecuteCommandEvent

internal class CommandListener
{
	@Listener(order = Order.POST)
	private fun onExecuteCommandEvent(event: ExecuteCommandEvent.Pre)
	{
		if (event.result().isPresent || event.command() != "stop")
		{
			return
		}
	}
}
