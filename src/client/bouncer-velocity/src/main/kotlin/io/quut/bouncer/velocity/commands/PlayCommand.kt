package io.quut.bouncer.velocity.commands

import com.google.protobuf.ByteString
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.JoinGameResponse
import io.quut.bouncer.grpc.joinGameRequest
import io.quut.bouncer.velocity.VelocityBouncerPluginLoader
import io.quut.bouncer.velocity.extensions.toByteArray
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

object PlayCommand
{
	fun createPlayCommand(plugin: VelocityBouncerPluginLoader, stub: BouncerGrpcKt.BouncerCoroutineStub): BrigadierCommand
	{
		return BrigadierCommand(
			BrigadierCommand.literalArgumentBuilder("play")
				.then(
					BrigadierCommand.requiredArgumentBuilder("gamemode", StringArgumentType.word())
						.executes()
						{ context ->
							val player: Player = context.source as Player? ?: return@executes 0

							val gamemode: String = context.getArgument("gamemode", String::class.java)

							player.sendMessage(Component.text("Requesting free game..", NamedTextColor.GRAY))

							runBlocking()
							{
								val response: JoinGameResponse = stub.joinGame(
									joinGameRequest()
								{
									this.gamemode = gamemode
									this.players.add(ByteString.copyFrom(player.uniqueId.toByteArray()))
								}
								)

								if (response.statusCase == JoinGameResponse.StatusCase.SUCCESS)
								{
									player.sendMessage(Component.text("Sending to server ${response.success.serverId}", NamedTextColor.GRAY))

									plugin.serversById[response.success.serverId]?.second.let()
									{ server ->
										player.createConnectionRequest(server).fireAndForget()
									}
								}
								else
								{
									player.sendMessage(Component.text("Failed :(", NamedTextColor.GRAY))
								}
							}

							return@executes Command.SINGLE_SUCCESS
						}
				)
		)
	}
}
