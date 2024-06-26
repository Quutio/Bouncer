package io.quut.bouncer.sponge.listeners

import io.quut.bouncer.api.IBouncerAPI
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.Order
import org.spongepowered.api.event.command.ExecuteCommandEvent

internal class CommandListener(private val bouncer: IBouncerAPI)
{
	@Listener(order = Order.POST)
	private fun onExecuteCommandEvent(event: ExecuteCommandEvent.Pre)
	{
		if (event.result().isPresent || (event.command() != "stop" && event.command() != "minecraft:stop") || !event.commandCause().hasPermission("minecraft.command.stop"))
		{
			return
		}

		this.bouncer.shutdownGracefully()
	}

	@Listener(order = Order.POST)
	private fun onExecuteCommandEvent(event: ExecuteCommandEvent.Post)
	{
		if (!event.result().isSuccess || (event.command() != "stop" && event.command() != "minecraft:stop"))
		{
			return
		}

		this.bouncer.shutdownGracefully()
	}
}
