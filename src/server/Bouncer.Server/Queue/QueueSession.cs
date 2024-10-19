using Bouncer.Grpc;
using Bouncer.Server.Network;
using Grpc.Core;

namespace Bouncer.Server.Queue;

internal sealed class QueueSession(QueueManager queueManager, IAsyncStreamReader<UniverseQueueRequest> requestStream, IServerStreamWriter<UniverseQueueResponse> responseStream)
	: BiDirectionalConnection<UniverseQueueRequest, UniverseQueueResponse>(requestStream, responseStream)
{
	private readonly QueueManager queueManager = queueManager;

	private readonly Dictionary<int, RegisteredQueue> queuesByTrackingId = [];

	protected override async ValueTask<bool> HandleAsync(UniverseQueueRequest request)
	{
		switch (request.MessageCase)
		{
			case UniverseQueueRequest.MessageOneofCase.Join:
			{
				RegisteredQueue queue = this.queueManager.Create(this, [.. request.Join.Players.Select(p => new Guid(p.Span, bigEndian: true))]);

				this.queuesByTrackingId[request.Join.TrackingId] = queue;

				await this.WriteAndForgetAsync(new UniverseQueueResponse
				{
					RequestId = request.RequestId,
					ConfirmJoin = new UniverseQueueResponse.Types.ConfirmJoin
					{
						Success = new UniverseQueueResponse.Types.ConfirmJoin.Types.Success
						{
							QueueId = queue.Id
						}
					}
				}).ConfigureAwait(false);

				this.queueManager.Join(queue, request.Join.UniverseType);

				break;
			}

			case UniverseQueueRequest.MessageOneofCase.ConfirmPrepare:
			{
				this.ReceivedResponseRequest(request.RequestId, request.ConfirmPrepare);

				break;
			}
		}

		return true;
	}

	protected override void PrepareResponse(UniverseQueueResponse response, int messageId)
	{
		response.RequestId = messageId;
	}

	internal Task<UniverseQueueRequest.Types.ConfirmPrepapre> WriteAsync(UniverseQueueResponse.Types.Prepapre prepare)
	{
		return this.WriteAsync<UniverseQueueRequest.Types.ConfirmPrepapre>(new UniverseQueueResponse
		{
			Prepare = prepare
		});
	}

	internal Task WriteAndForgetAsync(UniverseQueueResponse.Types.Confirmation confirmation)
	{
		return this.WriteAndForgetAsync(new UniverseQueueResponse
		{
			Confirmation = confirmation
		});
	}
}
