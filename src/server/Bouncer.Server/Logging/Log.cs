namespace Bouncer.Server.Logging;

internal static partial class Log
{
	[LoggerMessage(EventId = 1, Level = LogLevel.Debug, Message = "Registered server: {name} (Id: {id}) | (Total: {totalServers})")]
	internal static partial void ServerRegistered(this ILogger logger, string name, uint id, int totalServers);

	[LoggerMessage(EventId = 2, Level = LogLevel.Debug, Message = "Unregistered server: {name} (Id: {id}) | (Total: {totalServers})")]
	internal static partial void ServerUnregistered(this ILogger logger, string name, uint id, int totalServers);

	[LoggerMessage(EventId = 3, Level = LogLevel.Debug, Message = "Player {uniqueId} joined the server {serverName} (Id: {serverId}) | (Total: {totalPlayers})")]
	internal static partial void PlayerJoinServer(this ILogger logger, Guid uniqueId, string serverName, uint serverId, int totalPlayers);

	[LoggerMessage(EventId = 4, Level = LogLevel.Debug, Message = "Player {uniqueId} quit the server {serverName} (Id: {serverId}) | (Total: {totalPlayers})")]
	internal static partial void PlayerQuitServer(this ILogger logger, Guid uniqueId, string serverName, uint serverId, int totalPlayers);

	[LoggerMessage(EventId = 5, Level = LogLevel.Debug, Message = "Player {uniqueId} reserved a slot from the server {serverName} (Id: {serverId}) | (Total: {totalPlayers})")]
	internal static partial void PlayerReserveSlotServer(this ILogger logger, Guid uniqueId, string serverName, uint serverId, int totalPlayers);

	[LoggerMessage(EventId = 6, Level = LogLevel.Debug, Message = "Slot reserved for the player {uniqueId} on a server {serverName} (Id: {serverId}) timed out | (Total: {totalPlayers})")]
	internal static partial void PlayerReserveSlotTimeoutServer(this ILogger logger, Guid uniqueId, string serverName, uint serverId, int totalPlayers);
}
