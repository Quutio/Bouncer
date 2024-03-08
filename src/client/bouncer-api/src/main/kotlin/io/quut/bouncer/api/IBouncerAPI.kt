package io.quut.bouncer.api

import io.quut.bouncer.api.server.BouncerServerInfo
import io.quut.bouncer.api.server.IServerManager

interface IBouncerAPI
{
	val serverManager: IServerManager

	fun allServers(): Map<String, BouncerServerInfo>
	fun serversByGroup(group: String): Map<String, BouncerServerInfo>
	fun serverByName(name: String): BouncerServerInfo?

	fun shutdownGracefully()

	companion object
	{
		private lateinit var instance: IBouncerAPI

		@JvmStatic
		val api: IBouncerAPI
			get() = instance

		fun setApi(instance: IBouncerAPI)
		{
			Companion.instance = instance
		}
	}
}
