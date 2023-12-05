using System;
using System.Collections.Generic;
using System.Linq;
using System.Threading;
using System.Threading.Tasks;
using Bouncer.Grpc;
using Bouncer.Server.Server;
using Bouncer.Server.Server.Filter;
using Bouncer.Server.Server.Listener;
using Bouncer.Server.Server.Sort;
using Google.Protobuf.Collections;
using Grpc.Core;

namespace Bouncer.Server.Services;

internal sealed class GrpcServerService : ServerService.ServerServiceBase
{
	private readonly ServerManager serverManager;

	public GrpcServerService(ServerManager serverManager)
	{
		this.serverManager = serverManager;
	}

	public override async Task Session(IAsyncStreamReader<ServerSessionRequest> requestStream, IServerStreamWriter<ServerSessionResponse> responseStream, ServerCallContext context)
	{
		Dictionary<uint, RegisteredServer> servers = new();
			
		try
		{
			using CancellationTokenSource timeoutTokenSource = CancellationTokenSource.CreateLinkedTokenSource(context.CancellationToken);
			//await using Timer timeoutTimer = new(static state => ((CancellationTokenSource)state!).Cancel(), timeoutTokenSource, TimeSpan.FromSeconds(5), Timeout.InfiniteTimeSpan);

			while (await requestStream.MoveNext(timeoutTokenSource.Token))
			{
				ServerSessionRequest request = requestStream.Current;

				switch (request.RequestCase)
				{
					case ServerSessionRequest.RequestOneofCase.Ack:
					{
						break;
					}
					case ServerSessionRequest.RequestOneofCase.Registration:
					{
						RegisteredServer server = this.serverManager.Register(request.Registration.Data);

						servers[server.Id] = server;

						await responseStream.WriteAsync(new ServerSessionResponse
						{
							RequestId = request.RequestId,

							Registration = new ServerRegistrationResponse
							{
								ServerId = (int)server.Id
							}
						}, timeoutTokenSource.Token);

						break;
					}
					case ServerSessionRequest.RequestOneofCase.Unregistration:
					{
						uint serverId = (uint)request.Unregistration.ServerId;
						if (!servers.Remove(serverId, out RegisteredServer? server))
						{
							continue;
						}
								
						this.serverManager.Unregister(server);

						await responseStream.WriteAsync(new ServerSessionResponse
						{
							RequestId = request.RequestId
						}, timeoutTokenSource.Token);

						break;
					}
					case ServerSessionRequest.RequestOneofCase.Update:
					{
						if (!servers.TryGetValue((uint)request.Update.ServerId, out RegisteredServer? server))
						{
							continue;
						}

						this.Update(server, request.Update);

						await responseStream.WriteAsync(new ServerSessionResponse
						{
							RequestId = request.RequestId
						}, timeoutTokenSource.Token);

						break;
					}
				}
			}
		}
		catch (OperationCanceledException)
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
		switch (update.UpdateCase)
		{
			case ServerStatusUpdate.UpdateOneofCase.UserJoin:
			{
				server.Join(Guid.Parse(update.UserJoin.User));

				break;
			}
			case ServerStatusUpdate.UpdateOneofCase.UserQuit:
			{
				server.Quit(Guid.Parse(update.UserQuit.User));

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

			foreach (string user in request.User)
			{
				server.ReserveSlot(Guid.Parse(user));
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

			await listener.Listen(responseStream, context.CancellationToken);
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
			return filter.ConditionCase switch
			{
				ServerFilter.ConditionOneofCase.Name => new ServerNameFilter(filter.Name.Value),
				ServerFilter.ConditionOneofCase.Group => new ServerGroupFilter(filter.Group.Value),
				ServerFilter.ConditionOneofCase.Type => new ServerTypeFilter(filter.Type.Value),

				_ => throw new NotSupportedException($"Unsupported filter: {filter.ConditionCase}")
			};
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