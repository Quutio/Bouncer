package io.quut.bouncer.bukkit.listeners

import io.quut.bouncer.api.IBouncerAPI
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.server.ServerCommandEvent

internal class CommandListener(private val bouncer: IBouncerAPI) : Listener
{
	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	fun onServerCommand(event: ServerCommandEvent)
	{
		if (event.command != "stop")
		{
			return
		}

		this.bouncer.shutdownGracefully()
	}
}
