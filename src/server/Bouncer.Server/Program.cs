using Bouncer.Server.Games;
using Bouncer.Server.Server;
using Bouncer.Server.Services;

WebApplicationBuilder builder = WebApplication.CreateBuilder(args);

builder.Services.Configure<HostOptions>(options =>
{
	//We should not execute much shutdown logic so force early timeout
	options.ShutdownTimeout = TimeSpan.FromSeconds(3);
});

builder.Services.AddGrpc();

builder.Services.AddSingleton<ServerManager>();
builder.Services.AddSingleton<GameManager>();

builder.Services.AddSingleton<GrpcServerService>();
builder.Services.AddSingleton<GrpcGameService>();
builder.Services.AddSingleton<GrpcQueueService>();

WebApplication app = builder.Build();

if (builder.Environment.IsDevelopment())
{
	app.UseDeveloperExceptionPage();
}

app.UseRouting();

app.MapGrpcService<GrpcServerService>();
app.MapGrpcService<GrpcGameService>();
app.MapGrpcService<GrpcQueueService>();

app.MapGet("/", async context =>
{
	await context.Response.WriteAsync("Communication with gRPC endpoints must be made through a gRPC client. To learn how to create a client, visit: https://go.microsoft.com/fwlink/?linkid=2086909").ConfigureAwait(false);
});

await app.RunAsync().ConfigureAwait(false);
