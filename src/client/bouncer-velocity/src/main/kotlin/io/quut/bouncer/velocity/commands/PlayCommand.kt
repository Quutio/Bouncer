package io.quut.bouncer.velocity.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.velocity.universe.VelocityDistributedUniverseManager
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

internal object PlayCommand
{
	fun createPlayCommand(universeManager: VelocityDistributedUniverseManager): BrigadierCommand = BrigadierCommand(
		BrigadierCommand.literalArgumentBuilder("play")
			.requires { source -> source is Player && source.hasPermission("bouncer.command.play") }
			.then(BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.string())
				.executes()
				{ context ->
					val player: Player = context.source as Player

					val type: String = context.getArgument("type", String::class.java)

					player.sendMessage(Component.text("Requesting free universe..", NamedTextColor.GRAY))

					universeManager.join(IDistributedUniverseJoinRequest.of(Key.key(type), listOf(player.uniqueId))).thenAccept()
					{ result ->
						if (result)
						{
							player.sendMessage(Component.text("Successfully joined universe!", NamedTextColor.GREEN))
						}
						else
						{
							player.sendMessage(Component.text("Failed to join universe.", NamedTextColor.RED))
						}
					}

					return@executes Command.SINGLE_SUCCESS
				}))
}
