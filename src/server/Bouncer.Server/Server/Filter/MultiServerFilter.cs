using System.Collections.Generic;

namespace Bouncer.Server.Server.Filter
{
	internal sealed class MultiServerFilter : IServerFilter
	{
		private readonly List<IServerFilter> filters;

		internal MultiServerFilter(List<IServerFilter> filters)
		{
			this.filters = filters;
		}

		public bool Filter(RegisteredServer server)
		{
			foreach (IServerFilter serverFilter in this.filters)
			{
				if (!serverFilter.Filter(server))
				{
					return false;
				}
			}

			return true;
		}
	}
}
