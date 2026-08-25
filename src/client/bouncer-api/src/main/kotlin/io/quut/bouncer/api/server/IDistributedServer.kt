package io.quut.bouncer.api.server

import io.quut.bouncer.api.entity.IDistributedEntity

interface IDistributedServer : IDistributedEntity
{
	val info: IDistributedServerInfo
}
