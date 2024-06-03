using System.Collections.Immutable;
using System.Runtime.InteropServices;
using Bouncer.Grpc;

namespace Bouncer.Server.Queue;

internal sealed class QueueMatchSession(ImmutableArray<RegisteredQueue> queues)
{
	private readonly Dictionary<QueueSession, HashSet<RegisteredQueue>> queuesBySession = QueueMatchSession.GroupBySession(queues);

	internal ImmutableArray<RegisteredQueue> Queues { get; } = queues;

	internal async Task TryEstablish()
	{
		List<Task<GameQueueRequest.Types.ConfirmPrepapre>> prepares = [];
		foreach ((QueueSession session, HashSet<RegisteredQueue> sessionQueues) in this.queuesBySession)
		{
			prepares.Add(session.WriteAsync(new GameQueueResponse.Types.Prepapre
			{
				QueueIds = { sessionQueues.Select(i => i.Id) }
			}));
		}

		await Task.WhenAll(prepares);

		foreach ((QueueSession session, HashSet<RegisteredQueue> sessionQueues) in this.queuesBySession)
		{
			_ = session.WriteAndForgetAsync(new GameQueueResponse.Types.Confirmation
			{
				QueueIds = { sessionQueues.Select(i => i.Id) }
			});
		}
	}

	private static Dictionary<QueueSession, HashSet<RegisteredQueue>> GroupBySession(ImmutableArray<RegisteredQueue> queues)
	{
		Dictionary<QueueSession, HashSet<RegisteredQueue>> groupBySession = [];
		foreach (RegisteredQueue queue in queues)
		{
			ref HashSet<RegisteredQueue>? sessionQueues = ref CollectionsMarshal.GetValueRefOrAddDefault(groupBySession, queue.Session, out _);
			sessionQueues ??= [];

			sessionQueues.Add(queue);
		}

		return groupBySession;
	}
}
