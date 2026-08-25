package io.quut.bouncer.common.universe

import com.google.protobuf.ByteString
import io.quut.bouncer.api.universe.IDistributedUniverseJoinRequest
import io.quut.bouncer.api.universe.IDistributedUniverseManager
import io.quut.bouncer.common.extensions.toByteArray
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.grpc.JoinUniverseResponse
import io.quut.bouncer.grpc.joinUniverseRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.future.future
import java.util.concurrent.CompletableFuture

abstract class DistributedUniverseManager(private val networkManager: NetworkManager) : IDistributedUniverseManager
{
	override fun join(request: IDistributedUniverseJoinRequest): CompletableFuture<Boolean> = CoroutineScope(Dispatchers.Default).future()
	{
		val response: JoinUniverseResponse = this@DistributedUniverseManager.networkManager.stub.joinUniverse(joinUniverseRequest()
		{
			this.universeType = request.id.toString()
			this.players.addAll(request.players.map { player -> ByteString.copyFrom(player.toByteArray()) })
		})

		return@future response.statusCase == JoinUniverseResponse.StatusCase.SUCCESS
			&& this@DistributedUniverseManager.join(request, response)
	}

	protected abstract fun join(request: IDistributedUniverseJoinRequest, response: JoinUniverseResponse): Boolean
}
