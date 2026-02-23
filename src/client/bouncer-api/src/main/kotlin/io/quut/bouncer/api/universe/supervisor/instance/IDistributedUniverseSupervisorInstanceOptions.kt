package io.quut.bouncer.api.universe.supervisor.instance

interface IDistributedUniverseSupervisorInstanceOptions
{
	val info: IDistributedUniverseSupervisorInstanceInfo

	companion object
	{
		@JvmStatic
		fun of(info: IDistributedUniverseSupervisorInstanceInfo): IDistributedUniverseSupervisorInstanceOptions = Impl(info)
	}

	private class Impl(override var info: IDistributedUniverseSupervisorInstanceInfo) : IDistributedUniverseSupervisorInstanceOptions
}
