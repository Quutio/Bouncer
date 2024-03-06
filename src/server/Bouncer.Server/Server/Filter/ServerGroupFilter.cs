namespace Bouncer.Server.Server.Filter;

internal sealed class ServerGroupFilter : IServerFilter
{
	private readonly string group;

	internal ServerGroupFilter(string group)
	{
		this.group = group;
	}

	public bool Filter(RegisteredServer server) => server.Data.Group == this.group;
}
