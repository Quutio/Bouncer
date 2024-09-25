package io.quut.bouncer.api

import io.quut.bouncer.api.server.IBouncerServerInfo

interface IBouncerAPI
{
	@Deprecated("Only works on the proxy, replacement wip.")
	fun allServers(): Map<String, IBouncerServerInfo> = throw UnsupportedOperationException("Only supported on proxy")

	@Deprecated("Only works on the proxy, replacement wip.")
	fun serversByGroup(group: String): Map<String, IBouncerServerInfo> = throw UnsupportedOperationException("Only supported on proxy")

	@Deprecated("Only works on the proxy, replacement wip.")
	fun serverByName(name: String): IBouncerServerInfo? = throw UnsupportedOperationException("Only supported on proxy")

	companion object
	{
		private var instance: IBouncer? = null

		@JvmStatic
		fun get(): IBouncer = this.instance ?: throw IllegalStateException("Bouncer is not initialized")

		fun register(instance: IBouncer)
		{
			if (this.instance != null)
			{
				throw IllegalStateException("Already registered")
			}

			this.instance = instance
		}

		fun unregister(instance: IBouncer)
		{
			if (this.instance != instance)
			{
				throw IllegalArgumentException("Mismatched instance")
			}

			this.instance = null
		}
	}
}
