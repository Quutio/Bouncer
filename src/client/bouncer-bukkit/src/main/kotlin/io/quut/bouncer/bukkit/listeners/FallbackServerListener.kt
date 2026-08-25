package io.quut.bouncer.bukkit.listeners

import io.quut.bouncer.api.server.IDistributedServerContainer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

internal object FallbackServerListener
{
	internal class Accept(private val bouncerServer: IDistributedServerContainer) : Listener
	{
		@EventHandler(priority = EventPriority.MONITOR)
		fun onPlayerJoin(event: PlayerJoinEvent)
		{
			val player: Player = event.player

			this.bouncerServer.confirmJoin(player.uniqueId)
		}

		@EventHandler(priority = EventPriority.MONITOR)
		fun onPlayerQuit(event: PlayerQuitEvent)
		{
			val player: Player = event.player

			this.bouncerServer.confirmLeave(player.uniqueId)
		}
	}
}
