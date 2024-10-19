package io.quut.bouncer.api.server

import java.net.InetSocketAddress

interface IBouncerServerInfo
{
	val name: String
	val group: String
	val type: String
	val address: InetSocketAddress
	val maxMemory: Int?

	companion object
	{
		@JvmStatic
		@JvmOverloads
		fun of(name: String, group: String, type: String, address: InetSocketAddress, maxMemory: Int? = null): IBouncerServerInfo =
			Impl(name, group, type, address, maxMemory)
	}

	private class Impl(
		override val name: String,
		override val group: String,
		override val type: String,
		override val address: InetSocketAddress,
		override val maxMemory: Int?) : IBouncerServerInfo
}
