package io.quut.bouncer.api.server

interface IServerManager
{
	fun registerServer(info: BouncerServerInfo): IBouncerServer
	fun unregisterServer(server: IBouncerServer)
}
