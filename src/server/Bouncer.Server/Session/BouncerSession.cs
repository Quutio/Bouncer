using Bouncer.Grpc;
using Bouncer.Server.Network;
using Bouncer.Server.Server;
using Bouncer.Server.Universes;
using Google.Protobuf;
using Google.Protobuf.WellKnownTypes;
using Grpc.Core;

namespace Bouncer.Server.Session;

internal sealed class BouncerSession(ServerManager serverManager, UniverseManager universeManager, IAsyncStreamReader<ClientSessionMessage> requestStream, IServerStreamWriter<ServerSessionMessage> responseStream)
	: BiDirectionalConnection<ClientSessionMessage, ServerSessionMessage>(requestStream, responseStream), IAsyncDisposable
{
	private readonly ServerManager serverManager = serverManager;
	private readonly UniverseManager universeManager = universeManager;

	private readonly Dictionary<int, RegisteredServer> serversByTrackingId = [];
	private readonly Dictionary<int, RegisteredUniverse> universesByTrackingId = [];

	private Timer? timeoutTimer;

	internal override async Task StartAsync(CancellationToken cancellationToken = default)
	{
		await this.WriteAndForgetAsync(new ServerSessionMessage
		{
			SettingsResponse = new ServerSessionMessage.Types.SettingsResponse
			{
				PingInterval = Duration.FromTimeSpan(TimeSpan.FromSeconds(1))
			}
		}).ConfigureAwait(false);

		using CancellationTokenSource timeoutTokenSource = CancellationTokenSource.CreateLinkedTokenSource(cancellationToken);
		await using Timer timeoutTimer = new(static state => ((CancellationTokenSource)state!).Cancel(), timeoutTokenSource, TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

		this.timeoutTimer = timeoutTimer;

		await base.StartAsync(timeoutTokenSource.Token).ConfigureAwait(false);
	}

	protected override async ValueTask<bool> HandleAsync(ClientSessionMessage request)
	{
		ServerSessionMessage? response = null;

		switch (request.MessageCase)
		{
			case ClientSessionMessage.MessageOneofCase.CloseRequest:
			{
				this.Close(request.CloseRequest.Intentional);

				return false;
			}

			case ClientSessionMessage.MessageOneofCase.PingRequest:
			{
				this.timeoutTimer!.Change(TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

				break;
			}

			case ClientSessionMessage.MessageOneofCase.RegisterServerRequest:
			{
				RegisteredServer server = this.serverManager.Register(this, request.RegisterServerRequest.Data, request.RegisterServerRequest.Status);

				this.serversByTrackingId[request.RegisterServerRequest.TrackingId] = server;

				response = new ServerSessionMessage
				{
					RegisterServerResponse = new ServerSessionMessage.Types.ServerRegistrationResponse
					{
						ServerId = (int)server.Id
					}
				};

				foreach (ClientSessionMessage.Types.UniverseRegistration universeRegistration in request.RegisterServerRequest.Universes)
				{
					RegisteredUniverse universe = this.universeManager.Register(this, server, universeRegistration.Data);

					this.universesByTrackingId[universeRegistration.TrackingId] = universe;

					response.RegisterServerResponse.Universes.Add(new ServerSessionMessage.Types.UniverseRegistrationResponse
					{
						UniverseId = (int)universe.Id
					});
				}

				break;
			}

			case ClientSessionMessage.MessageOneofCase.UnregisterServerRequest:
			{
				if (!this.serversByTrackingId.Remove(request.UnregisterServerRequest.TrackingId, out RegisteredServer? server))
				{
					break;
				}

				this.serverManager.Unregister(server, unregistration: true);

				break;
			}

			case ClientSessionMessage.MessageOneofCase.UpdateServerRequest:
			{
				if (!this.serversByTrackingId.TryGetValue(request.UpdateServerRequest.TrackingId, out RegisteredServer? server))
				{
					break;
				}

				this.Update(server, request.UpdateServerRequest.Status);

				break;
			}

			case ClientSessionMessage.MessageOneofCase.RegisterUniverseRequest:
			{
				if (!this.serversByTrackingId.TryGetValue(request.RegisterUniverseRequest.ServerTrackingId, out RegisteredServer? server))
				{
					break;
				}

				RegisteredUniverse universe = this.universeManager.Register(this, server, request.RegisterUniverseRequest.Registration.Data);

				this.universesByTrackingId[request.RegisterUniverseRequest.Registration.TrackingId] = universe;

				response = new ServerSessionMessage
				{
					RegisterUniverseResponse = new ServerSessionMessage.Types.UniverseRegistrationResponse
					{
						UniverseId = (int)universe.Id
					}
				};

				break;
			}

			case ClientSessionMessage.MessageOneofCase.UnregisterUniverseRequest:
			{
				if (!this.universesByTrackingId.TryGetValue(request.UnregisterUniverseRequest.TrackingId, out RegisteredUniverse? universe))
				{
					break;
				}

				this.universeManager.Unregister(universe);

				break;
			}

			case ClientSessionMessage.MessageOneofCase.ReserveResponse:
			{
				this.ReceivedResponseRequest(request.MessageId, request.ReserveResponse);

				break;
			}
		}

		if (response is not null)
		{
			if (request.HasMessageId)
			{
				response.MessageId = request.MessageId;
			}

			await this.WriteAndForgetAsync(response).ConfigureAwait(false);
		}

		return true;
	}

	private void Update(RegisteredServer server, ServerStatusUpdate update)
	{
		foreach (ByteString player in update.PlayersJoined)
		{
			server.Join(new Guid(player.Span, bigEndian: true));
		}

		foreach (ByteString player in update.PlayersLeft)
		{
			server.Quit(new Guid(player.Span, bigEndian: true));
		}

		if (update.HasTps)
		{
			server.Status.Tps = update.Tps;
		}

		if (update.HasMemory)
		{
			server.Status.Memory = update.Memory;
		}
	}

	protected override void PrepareResponse(ServerSessionMessage response, int messageId)
	{
		response.MessageId = messageId;
	}

	internal Task<ClientSessionMessage.Types.ReserveResponse> WriteAsync(ServerSessionMessage.Types.ReserveRequest request)
	{
		return this.WriteAsync<ClientSessionMessage.Types.ReserveResponse>(new ServerSessionMessage
		{
			ReserveRequest = request
		});
	}

	private void Close(bool intentional = false)
	{
		foreach (RegisteredServer server in this.serversByTrackingId.Values)
		{
			this.serverManager.Unregister(server, intentional);
		}

		foreach (RegisteredUniverse universe in this.universesByTrackingId.Values)
		{
			this.universeManager.Unregister(universe);
		}
	}

	public ValueTask DisposeAsync()
	{
		this.Close();

		return ValueTask.CompletedTask;
	}
}
