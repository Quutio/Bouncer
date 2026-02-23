package io.quut.bouncer.api.universe

interface IDistributedUniverseProvider
{
	fun registerUniverse(options: IDistributedUniverseOptions): IDistributedUniverseContainer
}
