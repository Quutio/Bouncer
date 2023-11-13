package fi.joniaromaa.bouncer.velocity

import fi.joniaromaa.bouncer.api.server.BouncerServerInfo
import fi.joniaromaa.bouncer.common.BouncerAPI

class VelocityBouncerAPI(val plugin: VelocityBouncerPlugin, endpoint: String) : BouncerAPI(endpoint) {

    override fun allServers(): Map<String, BouncerServerInfo> {
        return plugin.serversByName.toMap()
    }

    override fun serversByGroup(group: String): Map<String, BouncerServerInfo> {
        return plugin.serversByName.entries
            .filter { it.value.group == group }
            .associateBy({it.key}, {it.value})
    }

    override fun serverByName(name: String): BouncerServerInfo? {
        return plugin.serversByName[name]
    }

}