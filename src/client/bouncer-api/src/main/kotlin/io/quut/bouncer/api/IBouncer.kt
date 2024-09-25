package io.quut.bouncer.api

import io.quut.bouncer.api.server.IServerManager

interface IBouncer : IBouncerAPI
{
	val serverManager: IServerManager
}
