package io.quut.bouncer.api

import io.quut.bouncer.api.server.IBouncerServerInfo
import io.quut.bouncer.api.server.IServerManager

interface IBouncerAPI
{
	val serverManager: IServerManager

	fun allServers(): Map<String, IBouncerServerInfo>
	fun serversByGroup(group: String): Map<String, IBouncerServerInfo>
	fun serverByName(name: String): IBouncerServerInfo?

	fun shutdownGracefully()
	fun shutdownNow()

	companion object
	{
		private lateinit var instance: IBouncerAPI

		@JvmStatic
		val api: IBouncerAPI
			get() = this.instance

		fun setApi(instance: IBouncerAPI)
		{
			this.instance = instance
		}
	}
}
