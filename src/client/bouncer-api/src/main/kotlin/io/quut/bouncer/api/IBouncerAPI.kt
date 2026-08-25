package io.quut.bouncer.api

interface IBouncerAPI
{
	val bouncer: IBouncer

	companion object
	{
		private var instance: IBouncerAPI? = null

		@JvmStatic
		fun get(): IBouncerAPI = this.instance ?: throw IllegalStateException("Bouncer is not initialized")

		fun register(instance: IBouncerAPI)
		{
			if (this.instance != null)
			{
				throw IllegalStateException("Already registered")
			}

			this.instance = instance
		}

		fun unregister(instance: IBouncerAPI)
		{
			if (this.instance != instance)
			{
				throw IllegalArgumentException("Mismatched instance")
			}

			this.instance = null
		}
	}
}
