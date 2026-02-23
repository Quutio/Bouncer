package io.quut.bouncer.velocity

import com.google.inject.Inject
import io.quut.bouncer.common.BouncerPlugin
import io.quut.bouncer.common.config.IBouncerConfig
import io.quut.bouncer.common.network.NetworkManager
import io.quut.bouncer.velocity.server.VelocityDistributedServerManager

internal class VelocityBouncerPlugin @Inject constructor(networkManager: NetworkManager, serverManager: VelocityDistributedServerManager) : BouncerPlugin(networkManager, serverManager)
{
	override lateinit var config: IBouncerConfig

	override fun loadConfig()
	{
		this.config = object : IBouncerConfig
		{
			override val apiUrl: String
				get() = "localhost:5000"

			override val defaultServer: IBouncerConfig.IDefaultServer =
				object : IBouncerConfig.IDefaultServer
				{
					override val enabled: Boolean
						get() = true

					override val name: String
						get() = "proxy"
					override val group: String
						get() = "proxy"
					override val type: String
						get() = "velocity"
				}
		}
	}
}
