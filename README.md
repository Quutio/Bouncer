# Bouncer

A load balancer for Minecraft servers (and minigames). Microservice based, and uses gRPC server to communicate between the proxies and servers.

## Server (Microservice)
Written in C# and uses ASP.NET Core. Keeps track of active servers and which players are connected to them. Clients request for which server the player should be connected to based on the server group and/or type and sorting requirements.

## Client
Written in Kotlin and supports the Velocity and Bukkit platforms. The Bukkit plugin keeps open session to the microservice to let it know that the server is active and accepts connections. Whenever players join's or quit's, the microservice is informed. For the proxy, it listens for active/inactive servers.