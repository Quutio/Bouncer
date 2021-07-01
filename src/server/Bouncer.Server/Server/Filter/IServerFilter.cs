namespace Bouncer.Server.Server.Filter
{
	internal interface IServerFilter
	{
		bool Filter(RegisteredServer server);
	}
}
