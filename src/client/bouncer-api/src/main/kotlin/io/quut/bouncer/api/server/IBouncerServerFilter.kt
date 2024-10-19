package io.quut.bouncer.api.server

interface IBouncerServerFilter
{
	fun not(): INot = INot.of(this)

	interface INot : IBouncerServerFilter
	{
		val filter: IBouncerServerFilter

		companion object
		{
			fun of(filter: IBouncerServerFilter): INot = Impl(filter)
		}

		private class Impl(override val filter: IBouncerServerFilter) : INot
	}

	interface IGroup : IBouncerServerFilter
	{
		val group: String

		companion object
		{
			fun of(group: String): IGroup = Impl(group)
		}

		private class Impl(override val group: String) : IGroup
	}
}
