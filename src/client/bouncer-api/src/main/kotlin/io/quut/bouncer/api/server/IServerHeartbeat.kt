package io.quut.bouncer.api.server

interface IServerHeartbeat
{
	val tps: Double?
	val memory: Int?

	interface IBuilder
	{
		fun tps(tps: Double)
		fun memory(memory: Int)

		fun build(): IServerHeartbeat
	}

	companion object
	{
		fun builder(): IBuilder = BuilderImpl()
	}

	private class Impl(override val tps: Double?, override val memory: Int?) : IServerHeartbeat

	private class BuilderImpl : IBuilder
	{
		private var tps: Double? = null
		private var memory: Int? = null

		override fun tps(tps: Double)
		{
			this.tps = tps
		}

		override fun memory(memory: Int)
		{
			this.memory = memory
		}

		override fun build(): IServerHeartbeat = Impl(this.tps, this.memory)
	}
}
