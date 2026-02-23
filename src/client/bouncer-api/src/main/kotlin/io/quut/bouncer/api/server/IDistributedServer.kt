package io.quut.bouncer.api.server

import io.quut.bouncer.api.unit.IDistributedUnit

interface IDistributedServer : IDistributedUnit
{
	val info: IDistributedServerInfo
}
