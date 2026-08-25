using System.Collections.Concurrent;
using System.Threading.Channels;
using Grpc.Core;

namespace Bouncer.Server.Network;

internal abstract class BiDirectionalConnection<TRequest, TResponse>(IAsyncStreamReader<TRequest> requestStream, IServerStreamWriter<TResponse> responseStream)
{
	private readonly IAsyncStreamReader<TRequest> requestStream = requestStream;
	private readonly IServerStreamWriter<TResponse> responseStream = responseStream;

	private readonly Channel<TResponse> outgoingChannel = Channel.CreateUnbounded<TResponse>(new UnboundedChannelOptions
	{
		SingleReader = true
	});

	private int nextMessageIdRequestId;
	private readonly ConcurrentDictionary<int, ResponseCallback> messageCallbacks = [];

	internal virtual async Task StartAsync(CancellationToken cancellationToken = default)
	{
		try
		{
			_ = this.SendOutgoingMessages(cancellationToken);

			while (await this.requestStream.MoveNext(cancellationToken).ConfigureAwait(false))
			{
				if (await this.HandleAsync(this.requestStream.Current).ConfigureAwait(false))
				{
					continue;
				}

				return;
			}
		}
		catch (Exception e)
		{
			this.Complete(e);
		}
		finally
		{
			this.Complete();
		}
	}

	protected abstract ValueTask<bool> HandleAsync(TRequest request);

	internal async Task WriteAndForgetAsync(TResponse response)
	{
		await this.outgoingChannel.Writer.WriteAsync(response).ConfigureAwait(false);
	}

	protected async Task<TResponseRequest> WriteAsync<TResponseRequest>(TResponse response)
	{
		int messageId = Interlocked.Increment(ref this.nextMessageIdRequestId);

		this.PrepareResponse(response, messageId);

		TaskCompletionSource<TResponseRequest> taskCompletionSource = new();

		this.messageCallbacks[messageId] = new ResponseCallback<TResponseRequest>(taskCompletionSource);

		await this.outgoingChannel.Writer.WriteAsync(response).ConfigureAwait(false);

		return await taskCompletionSource.Task.ConfigureAwait(false);
	}

	protected abstract void PrepareResponse(TResponse response, int messageId);

	protected void ReceivedResponseRequest<TResponseRequest>(int messageId, TResponseRequest responseRequest)
	{
		((ResponseCallback<TResponseRequest>)this.messageCallbacks[messageId]).SetResult(responseRequest);
	}

	private async Task SendOutgoingMessages(CancellationToken cancellationToken = default)
	{
		try
		{
			ChannelReader<TResponse> reader = this.outgoingChannel.Reader;

			while (!cancellationToken.IsCancellationRequested)
			{
				if (!reader.TryRead(out TResponse? response))
				{
					await reader.WaitToReadAsync(cancellationToken).ConfigureAwait(false);

					continue;
				}

				await this.responseStream.WriteAsync(response, cancellationToken).ConfigureAwait(false);
			}
		}
		catch (Exception e)
		{
			this.Complete(e);
		}
	}

	private void Complete(Exception? e = null)
	{
		if (!this.outgoingChannel.Writer.TryComplete(e))
		{
			return;
		}

		foreach (ResponseCallback callback in this.messageCallbacks.Values)
		{
			if (e is not null)
			{
				callback.SetException(e);
			}
			else
			{
				callback.SetCanceled();
			}
		}
	}

	private abstract class ResponseCallback
	{
		internal abstract void SetException(Exception e);

		internal abstract void SetCanceled();
	}

	private sealed class ResponseCallback<T>(TaskCompletionSource<T> taskCompletionSource) : ResponseCallback
	{
		private readonly TaskCompletionSource<T> taskCompletionSource = taskCompletionSource;

		internal void SetResult(T result) => this.taskCompletionSource.SetResult(result);

		internal override void SetException(Exception e) => this.taskCompletionSource.SetException(e);

		internal override void SetCanceled() => this.taskCompletionSource.SetCanceled();
	}
}
