using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Session;
using Google.Protobuf;

namespace Bouncer.Server.Games;

internal sealed class RegisteredGame(BouncerSession session, RegisteredServer server, uint id, GameData gameData)
{
	private readonly BouncerSession session = session;

	internal RegisteredServer Server { get; } = server;

	public GameData Data { get; set; } = gameData;

	internal uint Id { get; } = id;

	public async Task<bool> ReserveSlot(Guid player)
	{
		ClientSessionMessage.Types.ReserveResponse response = await this.session.WriteAsync(new ServerSessionMessage.Types.ReserveRequest
		{
			GameId = (int)this.Id,
			Players = { ByteString.CopyFrom(player.ToByteArray(true)) }
		}).ConfigureAwait(false);

		switch (response.ResultCase)
		{
			case ClientSessionMessage.Types.ReserveResponse.ResultOneofCase.Success:
				return true;
		}

		return false;
	}
}
