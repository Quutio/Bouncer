package io.quut.bouncer.api.server

import java.net.InetSocketAddress

data class BouncerServerInfo(val name: String, val group: String, val type: String, val address: InetSocketAddress)
