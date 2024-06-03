package io.quut.bouncer.velocity.commands

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import io.quut.bouncer.velocity.queue.QueueManager
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object QueueCommand
{
	fun createQueueCommand(queueManager: QueueManager): BrigadierCommand
	{
		return BrigadierCommand(
			BrigadierCommand.literalArgumentBuilder("queue")
			.then(
				BrigadierCommand.literalArgumentBuilder("accept")
					.executes()
					{ context ->
						val player: Player = context.source as Player? ?: return@executes 0

						// queueManager.accept(player)

						return@executes Command.SINGLE_SUCCESS
					}
			).then(
				BrigadierCommand.literalArgumentBuilder("deny")
					.executes()
					{ context ->
						val player: Player = context.source as Player? ?: return@executes 0

						// queueManager.deny(player)

						return@executes Command.SINGLE_SUCCESS
					}
			).then(
				BrigadierCommand.requiredArgumentBuilder("gamemode", StringArgumentType.word())
					.executes()
					{ context ->
						val player: Player = context.source as Player? ?: return@executes 0

						val gamemode: String = context.getArgument("gamemode", String::class.java)

						queueManager.join(player.uniqueId, gamemode)

						player.sendMessage(Component.text("In queue..", NamedTextColor.GRAY))

						return@executes Command.SINGLE_SUCCESS
					}
			)
		)
	}
}
