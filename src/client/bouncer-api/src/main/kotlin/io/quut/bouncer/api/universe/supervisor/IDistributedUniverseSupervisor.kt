package io.quut.bouncer.api.universe.supervisor

import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo
import io.quut.fusion.api.connection.IConnectionRequestTemplate
import net.kyori.adventure.audience.Audience

interface IDistributedUniverseSupervisor
{
	fun join(audience: Audience, template: IConnectionRequestTemplate, info: IDistributedUniverseSupervisorInstanceInfo)
}
