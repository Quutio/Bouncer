using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Server.Filter;
using Bouncer.Server.Server.Listener;
using Bouncer.Server.Server.Sort;
using Google.Protobuf;
using Google.Protobuf.Collections;
using Google.Protobuf.WellKnownTypes;
using Grpc.Core;

namespace Bouncer.Server.Services;

internal sealed class GrpcBouncerService(ServerManager serverManager) : Grpc.Bouncer.BouncerBase
{
	private readonly ServerManager serverManager = serverManager;

	public override async Task Session(IAsyncStreamReader<BouncerSessionRequest> requestStream, IServerStreamWriter<BouncerSessionResponse> responseStream, ServerCallContext context)
	{
		await responseStream.WriteAsync(new BouncerSessionResponse
		{
			Settings = new BouncerSessionResponse.Types.Settings
			{
				PingInterval = Duration.FromTimeSpan(TimeSpan.FromSeconds(1))
			}
		});

		Dictionary<int, RegisteredServer> servers = [];

		try
		{
			using CancellationTokenSource timeoutTokenSource = CancellationTokenSource.CreateLinkedTokenSource(context.CancellationToken);
			await using Timer timeoutTimer = new(static state => ((CancellationTokenSource)state!).Cancel(), timeoutTokenSource, TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

			while (await requestStream.MoveNext(timeoutTokenSource.Token).ConfigureAwait(false))
			{
				BouncerSessionRequest request = requestStream.Current;

				switch (request.RequestCase)
				{
					case BouncerSessionRequest.RequestOneofCase.Close:
					{
						foreach (RegisteredServer server in servers.Values)
						{
							this.serverManager.Unregister(server, unregistration: request.Close.Intentional);
						}

						servers.Clear();

						return;
					}

					case BouncerSessionRequest.RequestOneofCase.Ping:
					{
						timeoutTimer.Change(TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

						break;
					}

					case BouncerSessionRequest.RequestOneofCase.ServerRegistration:
					{
						RegisteredServer server = this.serverManager.Register(request.ServerRegistration.Data, request.ServerRegistration.Status);

						servers[request.ServerRegistration.TrackingId] = server;

						if (request.HasRequestId)
						{
							await responseStream.WriteAsync(new BouncerSessionResponse
							{
								RequestId = request.RequestId,
								ServerRegistration = new BouncerSessionResponse.Types.ServerRegistration
								{
									ServerId = (int)server.Id
								}
							}, timeoutTokenSource.Token).ConfigureAwait(false);
						}

						break;
					}

					case BouncerSessionRequest.RequestOneofCase.ServerUnregistration:
					{
						if (!servers.Remove(request.ServerUnregistration.TrackingId, out RegisteredServer? server))
						{
							continue;
						}

						this.serverManager.Unregister(server, unregistration: true);

						if (request.HasRequestId)
						{
							await responseStream.WriteAsync(new BouncerSessionResponse
							{
								RequestId = request.RequestId
							}, timeoutTokenSource.Token).ConfigureAwait(false);
						}

						break;
					}

					case BouncerSessionRequest.RequestOneofCase.ServerUpdate:
					{
						if (!servers.TryGetValue(request.ServerUpdate.TrackingId, out RegisteredServer? server))
						{
							continue;
						}

						this.Update(server, request.ServerUpdate.Status);

						if (request.HasRequestId)
						{
							await responseStream.WriteAsync(new BouncerSessionResponse
							{
								RequestId = request.RequestId
							}, timeoutTokenSource.Token).ConfigureAwait(false);
						}

						break;
					}
				}
			}
		}
		catch (OperationCanceledException)
		{
			//Swallow
		}
		catch (IOException e) when (e.InnerException is OperationCanceledException)
		{
			//Swallow
		}
		finally
		{
			foreach (RegisteredServer server in servers.Values)
			{
				this.serverManager.Unregister(server);
			}
		}
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

	public override Task<ServerJoinResponse> JoinServer(ServerJoinRequest request, ServerCallContext context)
	{
		IServerFilter? filter = this.CreateFilter(request.Filter);
		IServerSort? sort = this.CreateSort(request.Sort);

		//TODO: Create dynamic sorted lists
		foreach (RegisteredServer server in this.serverManager.Servers.OrderBy(key => key, (IComparer<RegisteredServer>?)sort ?? Comparer<RegisteredServer>.Default))
		{
			if (filter is not null && !filter.Filter(server))
			{
				continue;
			}

			foreach (ByteString user in request.Players)
			{
				server.ReserveSlot(new Guid(user.Span, bigEndian: true));
			}

			return Task.FromResult(new ServerJoinResponse
			{
				Success = new ServerJoinResponse.Types.Success
				{
					ServerId = (int)server.Id
				}
			});
		}

		return Task.FromResult(new ServerJoinResponse
		{
			NoServers = new ServerJoinResponse.Types.NoServers()
		});
	}

	public override Task<ServerListResponse> ListServers(ServerListRequest request, ServerCallContext context)
	{
		ServerListResponse response = new();
		foreach (RegisteredServer server in this.serverManager.Servers)
		{
			ServerStatus status = new()
			{
				PlayerListHumanReadable = new PlayerListHumanReadable()
			};

			foreach (Guid player in server.Players)
			{
				status.PlayerListHumanReadable.Players.Add(player.ToString());
			}

			response.Servers.Add(new ServerDetails
			{
				ServerId = (int)server.Id,

				Data = server.Data,
				Status = status
			});
		}

		return Task.FromResult(response);
	}

	public override async Task Listen(BouncerListenRequest request, IServerStreamWriter<BouncerListenResponse> responseStream, ServerCallContext context)
	{
		try
		{
			ServerStatusListener listener = this.serverManager.CreateServerListener(this.CreateFilter(request.Server.Filter));

			await listener.Listen(responseStream, context.CancellationToken).ConfigureAwait(false);
		}
		catch (OperationCanceledException)
		{
			//Swallow
		}
	}

	private IServerFilter? CreateFilter(RepeatedField<ServerFilter> filters)
	{
		static IServerFilter GetFilter(ServerFilter filter)
		{
			IServerFilter instance = filter.ConditionCase switch
			{
				ServerFilter.ConditionOneofCase.Name => new ServerNameFilter(filter.Name.Value),
				ServerFilter.ConditionOneofCase.Group => new ServerGroupFilter(filter.Group.Value),
				ServerFilter.ConditionOneofCase.Type => new ServerTypeFilter(filter.Type.Value),

				_ => throw new NotSupportedException($"Unsupported filter: {filter.ConditionCase}")
			};

			return filter.Inverse ? new InverseFilter(instance) : instance;
		}

		if (filters.Count == 0)
		{
			return null;
		}
		else if (filters.Count == 1)
		{
			return GetFilter(filters[0]);
		}

		List<IServerFilter> serverFilters = new(filters.Count);

		foreach (ServerFilter filter in filters)
		{
			serverFilters.Add(GetFilter(filter));
		}

		return new MultiServerFilter(serverFilters);
	}

	private IServerSort? CreateSort(RepeatedField<ServerSort> sorts)
	{
		static IServerSort GetSort(ServerSort sort)
		{
			return sort.SortCase switch
			{
				ServerSort.SortOneofCase.ByPlayerCount => new ServerPlayerCountSorter(sort.ByPlayerCount.Value),

				_ => throw new NotSupportedException($"Unsupported sort: {sort.SortCase}")
			};
		}

		if (sorts.Count == 0)
		{
			return null;
		}
		else if (sorts.Count == 1)
		{
			return GetSort(sorts[0]);
		}

		throw new NotSupportedException("Multiple sorts is a no no yet");
	}
}
