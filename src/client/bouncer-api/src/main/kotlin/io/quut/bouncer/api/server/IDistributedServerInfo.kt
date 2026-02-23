package io.quut.bouncer.api.server

interface IDistributedServerInfo
{
	val name: String
	val group: String
	val type: String
	val maxMemory: Int?

	companion object
	{
		@JvmStatic
		@JvmOverloads
		fun of(name: String, group: String, type: String, maxMemory: Int? = null): IDistributedServerInfo =
			Impl(name, group, type, maxMemory)
	}

	private class Impl(
		override val name: String,
		override val group: String,
		override val type: String,
		override val maxMemory: Int?) : IDistributedServerInfo
}
