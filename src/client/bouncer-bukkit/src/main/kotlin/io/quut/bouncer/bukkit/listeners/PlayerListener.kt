package io.quut.bouncer.bukkit.listeners

import io.quut.bouncer.api.server.IBouncerServer
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent

internal class PlayerListener(private val bouncerServer: IBouncerServer) : Listener
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
