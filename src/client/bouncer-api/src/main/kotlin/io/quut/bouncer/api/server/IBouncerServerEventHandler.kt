package io.quut.bouncer.api.server

interface IBouncerServerEventHandler
{
	fun addServer(id: Int, server: IBouncerServerInfo)
	fun removeServer(id: Int, server: IBouncerServerInfo, reason: RemoveReason)

	enum class RemoveReason
	{
		UNSPECIFIED,
		UNREGISTER,
		TIMEOUT,
		ERROR
	}
}
