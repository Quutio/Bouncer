using Bouncer.Grpc;
using Bouncer.Server.Network;
using Grpc.Core;

namespace Bouncer.Server.Queue;

internal sealed class QueueSession(QueueManager queueManager, IAsyncStreamReader<GameQueueRequest> requestStream, IServerStreamWriter<GameQueueResponse> responseStream)
	: BiDirectionalConnection<GameQueueRequest, GameQueueResponse>(requestStream, responseStream)
{
	private readonly QueueManager queueManager = queueManager;

	private readonly Dictionary<int, RegisteredQueue> queuesByTrackingId = [];

	protected override async ValueTask<bool> HandleAsync(GameQueueRequest request)
	{
		switch (request.MessageCase)
		{
			case GameQueueRequest.MessageOneofCase.Join:
			{
				RegisteredQueue queue = this.queueManager.Create(this, [.. request.Join.Players.Select(p => new Guid(p.Span, bigEndian: true))]);

				this.queuesByTrackingId[request.Join.TrackingId] = queue;

				await this.WriteAndForgetAsync(new GameQueueResponse
				{
					RequestId = request.RequestId,
					ConfirmJoin = new GameQueueResponse.Types.ConfirmJoin
					{
						Success = new GameQueueResponse.Types.ConfirmJoin.Types.Success
						{
							QueueId = queue.Id
						}
					}
				}).ConfigureAwait(false);

				this.queueManager.Join(queue, request.Join.Gamemode);

				break;
			}

			case GameQueueRequest.MessageOneofCase.ConfirmPrepare:
			{
				this.ReceivedResponseRequest(request.RequestId, request.ConfirmPrepare);

				break;
			}
		}

		return true;
	}

	protected override void PrepareResponse(GameQueueResponse response, int messageId)
	{
		response.RequestId = messageId;
	}

	internal Task<GameQueueRequest.Types.ConfirmPrepapre> WriteAsync(GameQueueResponse.Types.Prepapre prepare)
	{
		return this.WriteAsync<GameQueueRequest.Types.ConfirmPrepapre>(new GameQueueResponse
		{
			Prepare = prepare
		});
	}

	internal Task WriteAndForgetAsync(GameQueueResponse.Types.Confirmation confirmation)
	{
		return this.WriteAndForgetAsync(new GameQueueResponse
		{
			Confirmation = confirmation
		});
	}
}
