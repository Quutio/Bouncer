package io.quut.bouncer.velocity.commands

import com.google.protobuf.ByteString
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.JoinUniverseResponse
import io.quut.bouncer.grpc.joinUniverseRequest
import io.quut.bouncer.velocity.extensions.toByteArray
import io.quut.bouncer.velocity.listeners.ServerLoginPluginListener
import io.quut.bouncer.velocity.server.DynamicServerEventHandler
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

internal object PlayCommand
{
	fun createPlayCommand(stub: BouncerGrpcKt.BouncerCoroutineStub, servers: DynamicServerEventHandler, listener: ServerLoginPluginListener): BrigadierCommand
	{
		return BrigadierCommand(
			BrigadierCommand.literalArgumentBuilder("play")
				.then(
					BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.string())
						.executes()
						{ context ->
							val player: Player = context.source as Player? ?: return@executes 0

							val type: String = context.getArgument("type", String::class.java)

							player.sendMessage(Component.text("Requesting free universe..", NamedTextColor.GRAY))

							runBlocking()
							{
								val response: JoinUniverseResponse = stub.joinUniverse(
									joinUniverseRequest()
									{
										this.universeType = type
										this.players.add(ByteString.copyFrom(player.uniqueId.toByteArray()))
									})

								if (response.statusCase == JoinUniverseResponse.StatusCase.SUCCESS)
								{
									player.sendMessage(Component.text("Sending to server ${response.success.serverId}", NamedTextColor.GRAY))

									servers[response.success.serverId]?.let()
									{ server ->
										listener.addConnection(player, response.success.reservationId)

										player.createConnectionRequest(server).fireAndForget()
									}
								}
								else
								{
									player.sendMessage(Component.text("Failed :(", NamedTextColor.GRAY))
								}
							}

							return@executes Command.SINGLE_SUCCESS
						})
		)
	}
}
