package io.quut.bouncer.api

interface IBouncerAPI
{
	companion object
	{
		private var instance: IBouncer? = null

		@JvmStatic
		fun get(): IBouncer = this.instance ?: throw IllegalStateException("Bouncer is not initialized")

		fun register(instance: IBouncer)
		{
			if (this.instance != null)
			{
				throw IllegalStateException("Already registered")
			}

			this.instance = instance
		}

		fun unregister(instance: IBouncer)
		{
			if (this.instance != instance)
			{
				throw IllegalArgumentException("Mismatched instance")
			}

			this.instance = null
		}
	}
}
