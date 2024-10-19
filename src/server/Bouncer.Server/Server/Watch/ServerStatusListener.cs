using System.Threading.Tasks.Dataflow;
using Bouncer.Grpc;
using Bouncer.Server.Server.Filter;
using Grpc.Core;

namespace Bouncer.Server.Server.Watch;

internal sealed class ServerWatcher
{
	private readonly ServerManager serverManager;

	private readonly IServerFilter? filter;

	private readonly BufferBlock<BouncerWatchResponse> updates;

	internal ServerWatcher(ServerManager serverManager, IServerFilter? filter)
	{
		this.serverManager = serverManager;

		this.filter = filter;

		this.updates = new BufferBlock<BouncerWatchResponse>();
	}

	internal void TryAddServer(RegisteredServer server)
	{
		if (this.filter is not null && !this.filter.Filter(server))
		{
			return;
		}

		//Send the status update first
		this.AddUpdate(new BouncerWatchResponse
		{
			Server = new BouncerWatchResponse.Types.Server
			{
				ServerId = (int)server.Id,
				Add = new BouncerWatchResponse.Types.Server.Types.Add
				{
					Data = server.Data
				}
			}
		});

		//TODO: Not needed atm
		CancellationTokenRegistration registration = server.AddWatcher(this);
	}

	internal void RemoveServer(RegisteredServer server)
	{
		this.AddUpdate(new BouncerWatchResponse
		{
			Server = new BouncerWatchResponse.Types.Server
			{
				ServerId = (int)server.Id,
				Remove = new BouncerWatchResponse.Types.Server.Types.Remove
				{
					Reason = server.Unregistration ? ServerRemovelReason.Unregistration : ServerRemovelReason.Unspecified
				}
			}
		});
	}

	internal void AddUpdate(BouncerWatchResponse update)
	{
		this.updates.SendAsync(update);
	}

	internal async Task Watch(IServerStreamWriter<BouncerWatchResponse> responseStream, CancellationToken cancellationToken)
	{
		try
		{
			while (true)
			{
				//Try to first receive sync to avoid the async overhead
				if (!this.updates.TryReceive(out BouncerWatchResponse? update))
				{
					update = await this.updates.ReceiveAsync(cancellationToken).ConfigureAwait(false);
				}

				await responseStream.WriteAsync(update, cancellationToken).ConfigureAwait(false);
			}
		}
		finally
		{
			this.serverManager.RemoveWatcher(this);
		}
	}
}
