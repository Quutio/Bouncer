package io.quut.bouncer.common.config

interface IBouncerConfig
{
	val apiUrl: String
	val defaultServer: IDefaultServer

	interface IDefaultServer
	{
		val enabled: Boolean
		val name: String
		val group: String
		val type: String
	}
}
