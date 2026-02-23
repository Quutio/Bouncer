package io.quut.bouncer.velocity.commands

import com.google.protobuf.ByteString
import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.StringArgumentType
import com.velocitypowered.api.command.BrigadierCommand
import com.velocitypowered.api.proxy.Player
import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisor
import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisorProvider
import io.quut.bouncer.grpc.BouncerGrpcKt
import io.quut.bouncer.grpc.JoinUniverseResponse
import io.quut.bouncer.grpc.joinUniverseRequest
import io.quut.bouncer.velocity.extensions.toByteArray
import io.quut.bouncer.velocity.listeners.ServerLoginPluginListener
import io.quut.bouncer.velocity.server.DynamicServerEventHandler
import io.quut.fusion.velocity.connection.VelocityConnectionRequestTemplate
import io.quut.fusion.velocity.player.VelocityFusionPlayer
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.key.Key
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import java.util.ServiceLoader

internal object PlayCommand
{
	private val supervisors: Map<Key, IDistributedUniverseSupervisor> = this.resolveSupervisors()

	fun createPlayCommand(stub: BouncerGrpcKt.BouncerCoroutineStub, servers: DynamicServerEventHandler, listener: ServerLoginPluginListener): BrigadierCommand
	{
		return BrigadierCommand(
			BrigadierCommand.literalArgumentBuilder("play")
				.then(
					BrigadierCommand.requiredArgumentBuilder("type", StringArgumentType.string())
						.executes()
						{ context ->
							val player: Player = context.source as? Player ?: return@executes 0

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
									servers.getServer(response.success.serverId)?.let()
									{ server ->
										servers.getUniverse(response.success.universeId)?.let()
										{ (_, supervisor) ->
											player.sendMessage(Component.text("Sending to server ${response.success.serverId}, universe ${response.success.universeId} with supervisor ${supervisor.type}", NamedTextColor.GRAY))

											this@PlayCommand.supervisors[supervisor.type]?.join(VelocityFusionPlayer(player), VelocityConnectionRequestTemplate(server, null), supervisor)

											listener.addConnection(player, response.success.reservationId)
										}
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

	private fun resolveSupervisors(): Map<Key, IDistributedUniverseSupervisor>
	{
		val map: MutableMap<Key, IDistributedUniverseSupervisor> = hashMapOf()

		ServiceLoader.load(IDistributedUniverseSupervisorProvider::class.java).forEach { e -> e.provide(map::put) }

		return map.toMap()
	}
}
