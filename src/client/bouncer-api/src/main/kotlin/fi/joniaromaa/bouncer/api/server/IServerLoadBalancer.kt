package fi.joniaromaa.bouncer.api.server

interface IServerLoadBalancer
{
	fun registerServer(info: BouncerServerInfo): IBouncerServer
	fun unregisterServer(server: IBouncerServer)
}