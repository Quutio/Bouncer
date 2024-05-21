using System.Threading.Tasks.Dataflow;
using Bouncer.Grpc;
using Bouncer.Server.Server.Filter;
using Grpc.Core;

namespace Bouncer.Server.Server.Listener;

internal sealed class ServerStatusListener
{
	private readonly ServerManager serverManager;

	private readonly IServerFilter? filter;

	private readonly BufferBlock<BouncerListenResponse> updates;

	internal ServerStatusListener(ServerManager serverManager, IServerFilter? filter)
	{
		this.serverManager = serverManager;

		this.filter = filter;

		this.updates = new BufferBlock<BouncerListenResponse>();
	}

	internal void TryAddServer(RegisteredServer server)
	{
		if (this.filter is not null && !this.filter.Filter(server))
		{
			return;
		}

		//Send the status update first
		this.AddUpdate(new BouncerListenResponse
		{
			Server = new BouncerListenResponse.Types.Server
			{
				ServerId = (int)server.Id,
				Add = new BouncerListenResponse.Types.Server.Types.Add
				{
					Data = server.Data
				}
			}
		});

		//TODO: Not needed atm
		CancellationTokenRegistration registration = server.AddListener(this);
	}

	internal void RemoveServer(RegisteredServer server)
	{
		this.AddUpdate(new BouncerListenResponse
		{
			Server = new BouncerListenResponse.Types.Server
			{
				ServerId = (int)server.Id,
				Remove = new BouncerListenResponse.Types.Server.Types.Remove
				{
					Reason = server.Unregistration ? ServerRemovelReason.Unregistration : ServerRemovelReason.Unspecified
				}
			}
		});
	}

	internal void AddUpdate(BouncerListenResponse update)
	{
		this.updates.SendAsync(update);
	}

	internal async Task Listen(IServerStreamWriter<BouncerListenResponse> responseStream, CancellationToken cancellationToken)
	{
		try
		{
			while (true)
			{
				//Try to first receive sync to avoid the async overhead
				if (!this.updates.TryReceive(out BouncerListenResponse? update))
				{
					update = await this.updates.ReceiveAsync(cancellationToken).ConfigureAwait(false);
				}

				await responseStream.WriteAsync(update, cancellationToken).ConfigureAwait(false);
			}
		}
		finally
		{
			this.serverManager.RemoveListener(this);
		}
	}
}
