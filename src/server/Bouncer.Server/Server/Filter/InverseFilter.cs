namespace Bouncer.Server.Server.Filter;

internal sealed class InverseFilter(IServerFilter filter) : IServerFilter
{
	public bool Filter(RegisteredServer server) => !filter.Filter(server);
}
