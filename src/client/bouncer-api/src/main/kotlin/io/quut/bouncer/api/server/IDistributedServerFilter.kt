package io.quut.bouncer.api.server

interface IDistributedServerFilter
{
	fun not(): INot = INot.of(this)

	interface INot : IDistributedServerFilter
	{
		val filter: IDistributedServerFilter

		companion object
		{
			@JvmStatic
			fun of(filter: IDistributedServerFilter): INot = Impl(filter)
		}

		private class Impl(override val filter: IDistributedServerFilter) : INot
	}

	interface IGroup : IDistributedServerFilter
	{
		val group: String

		companion object
		{
			@JvmStatic
			fun of(group: String): IGroup = Impl(group)
		}

		private class Impl(override val group: String) : IGroup
	}
}
