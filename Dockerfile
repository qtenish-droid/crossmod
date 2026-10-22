# The build of the bot.
#
# The sources of the bot live in the folder bot, this file builds them for the
# hosts that look for a Dockerfile in the root of the repository.
FROM mcr.microsoft.com/dotnet/sdk:8.0 AS build

WORKDIR /src

# The dependencies are restored first, so they stay in the cache of Docker
# while only the sources change.
COPY bot/CrossMod.Bot.csproj ./
RUN dotnet restore

COPY bot/ ./
RUN dotnet publish -c Release -o /app --no-restore

# The runtime of the bot.
FROM mcr.microsoft.com/dotnet/aspnet:8.0

WORKDIR /app
COPY --from=build /app ./

# The state of the bot is written into this folder. Mount a volume on it, the
# links of the members are lost otherwise.
VOLUME ["/app/data"]

ENV CROSSMOD_DATA_PATH=/app/data/state.json
ENV CROSSMOD_PORT=8080
ENV ASPNETCORE_URLS=http://+:8080

EXPOSE 8080

ENTRYPOINT ["dotnet", "CrossMod.Bot.dll"]
