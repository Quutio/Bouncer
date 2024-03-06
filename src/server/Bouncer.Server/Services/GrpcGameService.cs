using Bouncer.Grpc;
using Bouncer.Server.Games;
using Grpc.Core;

namespace Bouncer.Server.Services;

internal sealed class GrpcGameService(GameManager gameManager) : GameService.GameServiceBase
{
	private readonly GameManager gameManager = gameManager;

	public override Task<GameRegisterResponse> Register(GameRegisterRequest request, ServerCallContext context)
	{
		this.gameManager.Register((uint)request.ServerId, request.Data);

		return Task.FromResult(new GameRegisterResponse());
	}
}
