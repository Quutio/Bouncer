package io.quut.bouncer.common

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.quut.bouncer.api.IBouncerAPI
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IServerManager
import io.quut.bouncer.common.server.ServerManager
import io.quut.bouncer.grpc.ServerServiceGrpcKt
import java.util.concurrent.TimeUnit

open class BouncerAPI(endpoint: String) : IBouncerAPI
{
	private val channel: ManagedChannel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext().build()
	private val stub: ServerServiceGrpcKt.ServerServiceCoroutineStub = ServerServiceGrpcKt.ServerServiceCoroutineStub(this.channel)

	private val _serverManager: ServerManager = ServerManager(this.stub)

	override val serverManager: IServerManager
		get() = this._serverManager

	override fun allServers(): Map<String, BouncerServerInfo>
	{
		throw UnsupportedOperationException("Only supported on proxy")
	}

	override fun serversByGroup(group: String): Map<String, BouncerServerInfo>
	{
		throw UnsupportedOperationException("Only supported on proxy")
	}

	override fun serverByName(name: String): BouncerServerInfo?
	{
		throw UnsupportedOperationException("Only supported on proxy")
	}

	init
	{
		IBouncerAPI.setApi(this)
	}

	override fun shutdownGracefully()
	{
		this._serverManager.shutdown()

		this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
		this.channel.shutdownNow()
	}
}
