package io.quut.bouncer.api.universe.supervisor

import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo
import io.quut.fusion.api.connection.IConnectionRequestTemplate
import io.quut.fusion.api.player.IFusionPlayer

interface IDistributedUniverseSupervisor
{
	fun join(player: IFusionPlayer, template: IConnectionRequestTemplate, supervisor: IDistributedUniverseSupervisorInstanceInfo)
}
