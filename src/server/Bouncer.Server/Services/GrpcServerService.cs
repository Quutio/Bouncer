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

internal sealed class GrpcServerService(ServerManager serverManager) : ServerService.ServerServiceBase
{
	private readonly ServerManager serverManager = serverManager;

	public override async Task Session(IAsyncStreamReader<ServerSessionRequest> requestStream, IServerStreamWriter<ServerSessionResponse> responseStream, ServerCallContext context)
	{
		await responseStream.WriteAsync(new ServerSessionResponse
		{
			Settings = new ServerSessionSettings
			{
				PingInterval = Duration.FromTimeSpan(TimeSpan.FromSeconds(1))
			}
		});

		Dictionary<uint, RegisteredServer> servers = [];

		try
		{
			using CancellationTokenSource timeoutTokenSource = CancellationTokenSource.CreateLinkedTokenSource(context.CancellationToken);
			await using Timer timeoutTimer = new(static state => ((CancellationTokenSource)state!).Cancel(), timeoutTokenSource, TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

			while (await requestStream.MoveNext(timeoutTokenSource.Token).ConfigureAwait(false))
			{
				ServerSessionRequest request = requestStream.Current;

				switch (request.RequestCase)
				{
					case ServerSessionRequest.RequestOneofCase.Close:
					{
						foreach (RegisteredServer server in servers.Values)
						{
							this.serverManager.Unregister(server, unregistration: request.Close.Intentional);
						}

						servers.Clear();

						return;
					}

					case ServerSessionRequest.RequestOneofCase.Ping:
					{
						timeoutTimer.Change(TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

						break;
					}

					case ServerSessionRequest.RequestOneofCase.Registration:
					{
						RegisteredServer server = this.serverManager.Register(request.Registration.Data);

						servers[server.Id] = server;

						if (request.HasRequestId)
						{
							await responseStream.WriteAsync(new ServerSessionResponse
							{
								RequestId = request.RequestId,
								Registration = new ServerRegistrationResponse
								{
									ServerId = (int)server.Id
								}
							}, timeoutTokenSource.Token).ConfigureAwait(false);
						}

						break;
					}

					case ServerSessionRequest.RequestOneofCase.Unregistration:
					{
						uint serverId = (uint)request.Unregistration.ServerId;
						if (!servers.Remove(serverId, out RegisteredServer? server))
						{
							continue;
						}

						this.serverManager.Unregister(server, unregistration: true);

						if (request.HasRequestId)
						{
							await responseStream.WriteAsync(new ServerSessionResponse
							{
								RequestId = request.RequestId
							}, timeoutTokenSource.Token).ConfigureAwait(false);
						}

						break;
					}

					case ServerSessionRequest.RequestOneofCase.Update:
					{
						if (!servers.TryGetValue((uint)request.Update.ServerId, out RegisteredServer? server))
						{
							continue;
						}

						this.Update(server, request.Update);

						if (request.HasRequestId)
						{
							await responseStream.WriteAsync(new ServerSessionResponse
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

	private void Update(RegisteredServer server, ServerStatusUpdateRequest update)
	{
		switch (update.UpdateCase)
		{
			case ServerStatusUpdateRequest.UpdateOneofCase.UserJoin:
			{
				server.Join(new Guid(update.UserJoin.User.Span, bigEndian: true));

				break;
			}

			case ServerStatusUpdateRequest.UpdateOneofCase.UserQuit:
			{
				server.Quit(new Guid(update.UserQuit.User.Span, bigEndian: true));

				break;
			}

			case ServerStatusUpdateRequest.UpdateOneofCase.Heartbeat:
			{
				server.Data.Tps = update.Heartbeat.Tps;
				server.Data.Memory = update.Heartbeat.Memory;

				break;
			}
		}
	}

	public override Task<ServerJoinResponse> Join(ServerJoinRequest request, ServerCallContext context)
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

			foreach (ByteString user in request.User)
			{
				server.ReserveSlot(new Guid(user.Span, bigEndian: true));
			}

			return Task.FromResult(new ServerJoinResponse
			{
				Success = new ServerJoinSuccess
				{
					ServerId = (int)server.Id
				}
			});
		}

		return Task.FromResult(new ServerJoinResponse
		{
			NoServers = new ServerJoinNoServers()
		});
	}

	public override Task<ServerListResponse> List(ServerListRequest request, ServerCallContext context)
	{
		ServerListResponse response = new();
		foreach (RegisteredServer server in this.serverManager.Servers)
		{
			ServerListData serverListData = new()
			{
				ServerId = (int)server.Id,

				Data = server.Data
			};

			foreach (Guid player in server.Players)
			{
				serverListData.Players.Add(player.ToString());
			}

			response.Servers.Add(serverListData);
		}

		return Task.FromResult(response);
	}

	public override async Task Listen(ServerListenRequest request, IServerStreamWriter<ServerStatusUpdate> responseStream, ServerCallContext context)
	{
		try
		{
			ServerStatusListener listener = this.serverManager.CreateServerListener(this.CreateFilter(request.Filter));

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
