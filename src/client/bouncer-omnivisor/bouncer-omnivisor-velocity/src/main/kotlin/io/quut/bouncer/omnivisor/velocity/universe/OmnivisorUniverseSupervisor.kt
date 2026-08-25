package io.quut.bouncer.omnivisor.velocity.universe

import io.quut.bouncer.api.universe.supervisor.IDistributedUniverseSupervisor
import io.quut.bouncer.api.universe.supervisor.instance.IDistributedUniverseSupervisorInstanceInfo
import io.quut.fusion.api.connection.IConnectionRequestTemplate
import io.quut.omnivisor.api.IOmnivisorAPI
import net.kyori.adventure.audience.Audience

internal class OmnivisorUniverseSupervisor : IDistributedUniverseSupervisor
{
	override fun join(audience: Audience, template: IConnectionRequestTemplate, info: IDistributedUniverseSupervisorInstanceInfo) =
		template.connect(audience, IOmnivisorAPI.get().omnivisor.connectionRequestParameters(info.attributes["universe"]!!.toInt()))
}
