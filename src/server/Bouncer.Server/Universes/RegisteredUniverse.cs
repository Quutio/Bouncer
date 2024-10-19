using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Session;
using Google.Protobuf;
using Google.Protobuf.Collections;

namespace Bouncer.Server.Universes;

internal sealed class RegisteredUniverse(BouncerSession session, RegisteredServer server, uint id, UniverseData universeData)
{
	private readonly BouncerSession session = session;

	internal RegisteredServer Server { get; } = server;

	public UniverseData Data { get; set; } = universeData;

	internal uint Id { get; } = id;

	public async Task<int?> ReserveSlot(RepeatedField<ByteString> players)
	{
		ClientSessionMessage.Types.ReserveResponse response = await this.session.WriteAsync(new ServerSessionMessage.Types.ReserveRequest
		{
			UniverseId = (int)this.Id,
			Players = { players }
		}).ConfigureAwait(false);

		switch (response.ResultCase)
		{
			case ClientSessionMessage.Types.ReserveResponse.ResultOneofCase.Success:
				return response.Success.ReservationId;
		}

		return null;
	}
}
