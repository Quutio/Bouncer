using System.Collections.Immutable;

namespace Bouncer.Server.Queue;

internal sealed class RegisteredQueue(QueueSession session, int id, ImmutableArray<Guid> players)
{
	internal QueueSession Session { get; } = session;

	internal int Id { get; } = id;

	internal ImmutableArray<Guid> Players { get; } = players;
}
