using Bouncer.Grpc;
using Bouncer.Server.Games;
using Bouncer.Server.Queue;
using Bouncer.Server.Server;
using Bouncer.Server.Server.Filter;
using Bouncer.Server.Server.Listener;
using Bouncer.Server.Server.Sort;
using Bouncer.Server.Session;
using Google.Protobuf;
using Google.Protobuf.Collections;
using Grpc.Core;

namespace Bouncer.Server.Services;

internal sealed class GrpcBouncerService(ServerManager serverManager, GameManager gameManager, QueueManager queueManager) : Grpc.Bouncer.BouncerBase
{
	private readonly ServerManager serverManager = serverManager;
	private readonly GameManager gameManager = gameManager;
	private readonly QueueManager queueManager = queueManager;

	public override async Task Session(IAsyncStreamReader<ClientSessionMessage> requestStream, IServerStreamWriter<ServerSessionMessage> responseStream, ServerCallContext context)
	{
		await using BouncerSession session = new(this.serverManager, this.gameManager, requestStream, responseStream);

		try
		{
			await session.StartAsync(context.CancellationToken).ConfigureAwait(false);
		}
		catch (OperationCanceledException)
		{
			//Swallow
		}
		catch (IOException e) when (e.InnerException is OperationCanceledException)
		{
			//Swallow
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

	public override async Task<JoinGameResponse> JoinGame(JoinGameRequest request, ServerCallContext context)
	{
		foreach (RegisteredGame game in this.gameManager.Games.Where(g => g.Data.Gamemode == request.Gamemode))
		{
			foreach (ByteString player in request.Players)
			{
				await game.ReserveSlot(new Guid(player.Span, bigEndian: true));
			}

			return new JoinGameResponse
			{
				Success = new JoinGameResponse.Types.Success
				{
					ServerId = (int)game.Server.Id,
					GameId = (int)game.Id
				}
			};
		}

		return new JoinGameResponse
		{
			NoServers = new JoinGameResponse.Types.NoServers()
		};
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
		catch (IOException e) when (e.InnerException is OperationCanceledException)
		{
			//Swallow
		}
	}

	public override async Task GameQueue(IAsyncStreamReader<GameQueueRequest> requestStream, IServerStreamWriter<GameQueueResponse> responseStream, ServerCallContext context)
	{
		QueueSession session = new(this.queueManager, requestStream, responseStream);

		try
		{
			await session.StartAsync(context.CancellationToken).ConfigureAwait(false);
		}
		catch (OperationCanceledException)
		{
			//Swallow
		}
		catch (IOException e) when (e.InnerException is OperationCanceledException)
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
