namespace Bouncer.Server.Server.Filter;

internal sealed class ServerTypeFilter : IServerFilter
{
	private readonly string type;

	internal ServerTypeFilter(string type)
	{
		this.type = type;
	}

	public bool Filter(RegisteredServer server) => server.Data.Type == this.type;
}