package io.quut.bouncer.common

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.quut.bouncer.api.IBouncerAPI
import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IServerManager
import io.quut.bouncer.common.server.AbstractServerManager
import io.quut.bouncer.grpc.BouncerGrpcKt
import sun.misc.Signal
import java.util.concurrent.TimeUnit

abstract class BouncerAPI(endpoint: String) : IBouncerAPI
{
	private val channel: ManagedChannel = ManagedChannelBuilder.forTarget(endpoint).usePlaintext().build()
	private val stub: BouncerGrpcKt.BouncerCoroutineStub = BouncerGrpcKt.BouncerCoroutineStub(this.channel)

	private lateinit var _serverManager: AbstractServerManager

	override val serverManager: IServerManager
		get() = this._serverManager

	protected fun init()
	{
		this._serverManager = this.createServerManager(this.stub)
	}

	protected abstract fun createServerManager(stub: BouncerGrpcKt.BouncerCoroutineStub): AbstractServerManager

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

	fun installShutdownSignal()
	{
		Signal.handle(Signal("INT"))
		{ _ ->
			this._serverManager.shutdown(intentional = true)

			this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
			this.channel.shutdownNow()

			this.onShutdownSignal()
		}
	}

	protected abstract fun onShutdownSignal()

	override fun shutdownGracefully()
	{
		this._serverManager.shutdown(intentional = true)

		this.channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
		this.channel.shutdownNow()
	}

	override fun shutdownNow()
	{
		this._serverManager.shutdown()

		this.channel.shutdownNow()
	}
}
