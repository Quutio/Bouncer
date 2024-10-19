using System.Collections.Concurrent;
using System.Collections.Immutable;

namespace Bouncer.Server.Queue;

internal sealed class QueueManager
{
	private readonly ConcurrentDictionary<int, RegisteredQueue> queues = [];
	private readonly ConcurrentDictionary<string, ConcurrentBag<RegisteredQueue>> universeQueues = [];

	private int nextId;

	internal RegisteredQueue Create(QueueSession session, ImmutableArray<Guid> players)
	{
		int queueId = Interlocked.Increment(ref this.nextId);

		RegisteredQueue queue = new(session, queueId, players);

		this.queues[queue.Id] = queue;

		return queue;
	}

	internal void Join(RegisteredQueue queue, string universeType)
	{
		ConcurrentBag<RegisteredQueue> queues = this.universeQueues.GetOrAdd(universeType, _ => []);
		if (!queues.TryTake(out RegisteredQueue? anotherQueue))
		{
			queues.Add(queue);
			return;
		}

		this.PrepareMatch(queue, anotherQueue);
	}

	private void PrepareMatch(params RegisteredQueue[] queues)
	{
		QueueMatchSession matchSession = new([.. queues]);
		_ = matchSession.TryEstablish();
	}
}
