package fi.joniaromaa.bouncer.common

import fi.joniaromaa.bouncer.api.IBouncerAPI
import fi.joniaromaa.bouncer.api.game.IGameLoadBalancer
import fi.joniaromaa.bouncer.api.server.IServerLoadBalancer
import fi.joniaromaa.bouncer.common.game.GameLoadBalancer
import fi.joniaromaa.bouncer.common.server.ServerLoadBalancer
import fi.joniaromaa.bouncer.grpc.ServerServiceGrpcKt
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import java.util.concurrent.TimeUnit

class BouncerAPI(endpoint: String) : IBouncerAPI
{
	private val channel: ManagedChannel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext().build()
	private val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub = ServerServiceGrpcKt.ServerServiceCoroutineStub(this.channel)

	private val _serverLoadBalancer: ServerLoadBalancer = ServerLoadBalancer(this.stub)
	private val _gameLoadBalancer: IGameLoadBalancer = GameLoadBalancer()

	override val serverLoadBalancer: IServerLoadBalancer
		get() = this._serverLoadBalancer
	override val gameLoadBalancer: IGameLoadBalancer
		get() = this._gameLoadBalancer

	init
	{
		IBouncerAPI.setApi(this)
	}

	override fun shutdownGracefully()
	{
		this._serverLoadBalancer.shutdown()

		this.channel.shutdown().awaitTermination(1, TimeUnit.SECONDS)
	}
}