package io.quut.bouncer.common.network

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.quut.bouncer.grpc.BouncerGrpcKt
import java.util.concurrent.TimeUnit

class NetworkManager
{
	private lateinit var channel: ManagedChannel
	lateinit var stub: BouncerGrpcKt.BouncerCoroutineStub

	fun connect(endpoint: String)
	{
		this.channel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext().build()
		this.stub = BouncerGrpcKt.BouncerCoroutineStub(this.channel)
	}

	fun shutdown()
	{
		this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
		this.channel.shutdownNow()
	}

	fun shutdownNow()
	{
		this.channel.shutdownNow()
	}
}
