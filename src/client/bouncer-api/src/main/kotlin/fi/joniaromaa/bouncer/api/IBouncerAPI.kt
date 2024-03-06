package fi.joniaromaa.bouncer.api

import fi.joniaromaa.bouncer.api.game.IGameLoadBalancer
import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.api.server.IServerLoadBalancer

interface IBouncerAPI
{
	val serverLoadBalancer: IServerLoadBalancer
	val gameLoadBalancer: IGameLoadBalancer

	fun allServers(): Map<String, BouncerServerInfo>
	fun serversByGroup(group: String): Map<String, BouncerServerInfo>
	fun serverByName(name: String): BouncerServerInfo?

	fun shutdownGracefully()

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
