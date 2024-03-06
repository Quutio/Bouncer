namespace Bouncer.Server.Server.Filter;

internal sealed class ServerNameFilter : IServerFilter
{
	private readonly string name;

	internal ServerNameFilter(string name)
	{
		this.name = name;
	}

	public bool Filter(RegisteredServer server) => server.Name == this.name;
}
