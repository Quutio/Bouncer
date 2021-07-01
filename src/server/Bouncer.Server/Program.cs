using Microsoft.AspNetCore.Hosting;
using Microsoft.Extensions.Hosting;

namespace Bouncer.Server
{
	internal static class Program
	{
		private static void Main(string[] args)
		{
			Program.CreateHostBuilder(args)
			       .Build()
			       .Run();
		}

		private static IHostBuilder CreateHostBuilder(string[] args) =>
			Host.CreateDefaultBuilder(args)
			    .ConfigureWebHostDefaults(webBuilder =>
			    {
				    webBuilder.UseStartup<Startup>();
			    });
	}
}
