using Bouncer.Grpc;

namespace Bouncer.Server.Server.Sort;

internal sealed class ServerPlayerCountSorter : IServerSort
{
	private readonly ServerSort.Types.ByPlayerCount.Types.Order order;

	internal ServerPlayerCountSorter(ServerSort.Types.ByPlayerCount.Types.Order order)
	{
		this.order = order;
	}

	public int Compare(RegisteredServer? x, RegisteredServer? y)
	{
		int xCount = x?.Players.Count ?? 0;
		int yCount = y?.Players.Count ?? 0;

		if (this.order == ServerSort.Types.ByPlayerCount.Types.Order.Ascending)
		{
			return xCount - yCount;
		}
		else
		{
			return yCount - xCount;
		}
	}
}
