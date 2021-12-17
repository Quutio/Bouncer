using Bouncer.Server.Games;
using Bouncer.Server.Server;
using Bouncer.Server.Services;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Http;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Hosting;

namespace Bouncer.Server;

public sealed class Startup
{
	public void ConfigureServices(IServiceCollection services)
	{
		services.AddGrpc();

		services.AddSingleton<ServerManager>();
		services.AddSingleton<GameManager>();

		services.AddSingleton<GrpcServerService>();
		services.AddSingleton<GrpcGameService>();
		services.AddSingleton<GrpcQueueService>();
	}

	public void Configure(IApplicationBuilder app, IWebHostEnvironment env)
	{
		if (env.IsDevelopment())
		{
			app.UseDeveloperExceptionPage();
		}

		app.UseRouting();

		app.UseEndpoints(endpoints =>
		{
			endpoints.MapGrpcService<GrpcServerService>();
			endpoints.MapGrpcService<GrpcGameService>();
			endpoints.MapGrpcService<GrpcQueueService>();

			endpoints.MapGet("/", async context =>
			{
				await context.Response.WriteAsync("Communication with gRPC endpoints must be made through a gRPC client. To learn how to create a client, visit: https://go.microsoft.com/fwlink/?linkid=2086909");
			});
		});
	}
}