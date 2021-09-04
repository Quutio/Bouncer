package fi.joniaromaa.bouncer.sponge.listeners

import fi.joniaromaa.bouncer.api.server.IBouncerServer
import org.spongepowered.api.event.Listener
import org.spongepowered.api.event.network.ServerSideConnectionEvent

class PlayerListener(private val bouncerServer: IBouncerServer) {

    @Listener
    fun onJoin(event: ServerSideConnectionEvent.Join) {
        this.bouncerServer.confirmJoin(event.player().uniqueId())
    }

    @Listener
    fun onLeave(event: ServerSideConnectionEvent.Disconnect) {
        this.bouncerServer.confirmLeave(event.player().uniqueId())
    }
}