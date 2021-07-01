using Microsoft.Extensions.Logging;

namespace Bouncer.Server.Internal
{
	internal static class EventIds
	{
		internal static readonly EventId ServerRegistered = new(1, nameof(EventIds.ServerRegistered));
		internal static readonly EventId ServerUnregistered = new(2, nameof(EventIds.ServerUnregistered));

		internal static readonly EventId PlayerJoinServer = new(3, nameof(EventIds.PlayerJoinServer));
		internal static readonly EventId PlayerQuitServer = new(4, nameof(EventIds.PlayerQuitServer));
		internal static readonly EventId PlayerReserveSlotServer = new(5, nameof(EventIds.PlayerReserveSlotServer));
		internal static readonly EventId PlayerReserveSlotTimeoutServer = new(6, nameof(EventIds.PlayerReserveSlotTimeoutServer));
	}
}
