FROM mcr.microsoft.com/dotnet/aspnet:5.0
WORKDIR /app
EXPOSE 80
EXPOSE 443
COPY bin/Release/net5.0/publish /app

ENTRYPOINT ["dotnet", "Bouncer.Server.dll"]