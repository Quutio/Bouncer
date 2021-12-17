using Bouncer.Grpc;
using Grpc.Core;
using System.Threading;
using System.Threading.Tasks;
using System.Threading.Tasks.Dataflow;
using Bouncer.Server.Server.Filter;

namespace Bouncer.Server.Server.Listener;

internal sealed class ServerStatusListener
{
	private readonly ServerManager serverManager;

	private readonly IServerFilter? filter;
		
	private readonly BufferBlock<ServerStatusUpdate> updates;

	internal ServerStatusListener(ServerManager serverManager, IServerFilter? filter)
	{
		this.serverManager = serverManager;

		this.filter = filter;
			
		this.updates = new BufferBlock<ServerStatusUpdate>();
	}

	internal void TryAddServer(RegisteredServer server)
	{
		if (this.filter is not null && !this.filter.Filter(server))
		{
			return;
		}

		//Send the status update first
		this.AddUpdate(new ServerStatusUpdate
		{
			ServerId = (int)server.Id,

			Add = new ServerStatusAdd
			{
				Data = server.Data
			}
		});

		//TODO: Not needed atm
		CancellationTokenRegistration registration = server.AddListener(this);
	}

	internal void RemoveServer(RegisteredServer server)
	{
		this.AddUpdate(new ServerStatusUpdate
		{
			ServerId = (int)server.Id,

			Remove = new ServerStatusRemove()
		});
	}

	internal void AddUpdate(ServerStatusUpdate update)
	{
		this.updates.SendAsync(update);
	}

	internal async Task Listen(IServerStreamWriter<ServerStatusUpdate> responseStream, CancellationToken cancellationToken)
	{
		try
		{
			while (true)
			{
				//Try to first receive sync to avoid the async overhead
				if (!this.updates.TryReceive(out ServerStatusUpdate? update))
				{
					update = await this.updates.ReceiveAsync(cancellationToken);
				}

				await responseStream.WriteAsync(update);
			}
		}
		finally
		{
			this.serverManager.RemoveListener(this);
		}
	}
}