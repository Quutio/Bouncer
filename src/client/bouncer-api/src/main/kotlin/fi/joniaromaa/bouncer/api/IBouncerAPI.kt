package fi.joniaromaa.bouncer.api

import fi.joniaromaa.bouncer.api.game.IGameLoadBalancer
import fi.joniaromaa.bouncer.api.server.IServerLoadBalancer

interface IBouncerAPI
{
	val serverLoadBalancer: IServerLoadBalancer
	val gameLoadBalancer: IGameLoadBalancer

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