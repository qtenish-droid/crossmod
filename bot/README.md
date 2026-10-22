# CrossMod Bot

The bot of the CrossMod server. It welcomes the members, hands out the roles
and links a Discord account to a Minecraft account.

It is written in C# on .NET 8 and uses [Discord.Net](https://github.com/discord-net/Discord.Net).

## What it does

| Feature | Where |
|---|---|
| Welcome message for new members | `#welcome` channel |
| Reaction roles | every message can carry a rule |
| Account linking | `/link` plus the `.link <code>` command in the game |
| Announcements and releases | `/announce` and `/release`, or the build pipeline |

## Building and running it

The .NET 8 SDK is the only thing that is needed.

```bash
cd bot
dotnet build
dotnet run
```

The bot reads its settings from the environment and from the file
`bot/local.env`, which is ignored by git and by the Docker build. That file
is the easiest way to keep the token on the machine, it never has to be typed
into a terminal or into a chat:

```
CROSSMOD_DISCORD_TOKEN=your-token
CROSSMOD_API_KEY=a-long-random-string
CROSSMOD_GUILD_ID=123456789012345678
```

A ready made file with all the keys and a short explanation of every one of
them is in `bot/local.env`. Without a token the bot refuses to start and names
the variables that are missing.

The settings of the file win over the environment, so a token in `local.env` is
never overwritten by a variable of the host by accident.

## Creating the application

1. Open the [developer portal of Discord](https://discord.com/developers/applications)
   and press **New Application**.
2. Name it *CrossMod* and press **Create**.
3. Open **Bot** in the sidebar and press **Reset Token**. The token is shown
   exactly once, it is the `CROSSMOD_DISCORD_TOKEN` of the bot.
4. Under **Privileged Gateway Intents** switch on **Server Members Intent**.
   The welcome messages need it, and Discord only hands it out to bots that
   are in at least one server.
5. Open **OAuth2 > URL Generator**, tick `bot` and `applications.commands` and
   use the permissions below. The generated link invites the bot.

| Permission | Why |
|---|---|
| Send Messages, Embed Links | the welcome message, the announcements |
| Read Message History | the reaction roles |
| Add Reactions | the reaction roles |
| Manage Roles | the verified role, the reaction roles |
| View Channel | reading the channels it posts in |

The permission bitmask of that list is `2416004160`, so the link is

```
https://discord.com/oauth2/authorize?client_id=YOUR_CLIENT_ID&permissions=2416004176&scope=bot%20applications.commands
```

## The settings of the bot

Every setting is an environment variable, which is what the free hosts inject.
The file `bot/local.env` holds the same keys and is used on a local machine.

| Variable | Meaning |
|---|---|
| `CROSSMOD_DISCORD_TOKEN` | the token of the application, required |
| `CROSSMOD_API_KEY` | the shared secret of the HTTP endpoints, required |
| `CROSSMOD_GUILD_ID` | the server the commands are registered in |
| `CROSSMOD_WELCOME_CHANNEL_ID` | the channel that welcomes new members |
| `CROSSMOD_ANNOUNCEMENT_CHANNEL_ID` | the channel that receives the announcements |
| `CROSSMOD_VERIFIED_ROLE_ID` | the role of a linked member |
| `CROSSMOD_STAFF_ROLE_ID` | the role that may use the staff commands |
| `CROSSMOD_GITHUB_SECRET` | the secret of the GitHub webhook, optional |
| `CROSSMOD_DATA_PATH` | the file the state is stored in, `data/state.json` |
| `CROSSMOD_PORT` | the port of the HTTP endpoints, `8080` |
| `CROSSMOD_OFFLINE` | `true` starts the bot without a connection to Discord |
| `CROSSMOD_MEMBERS_INTENT` | `false` leaves out the privileged members intent |
| `CROSSMOD_PUBLIC_URL` | the address the bot is reachable at |

**The guild id matters.** Without it the commands are registered globally and
Discord needs up to an hour to publish them everywhere. With it they appear in
the server right away. The id is the first number of the channel link of the
server, right click the channel and copy the id with the developer mode turned
on.

## The permissions of the channels

A channel may not give the permission to send messages to `@everyone`, and the
role of the bot does not get that permission back in the channel. The bot needs
its own entry in the permissions of the channel:

| Permission | For which channel |
|---|---|
| Send Messages | every channel the bot writes in |
| View Channel | every channel the bot reads |
| Manage Roles | the verified role and the reaction roles |

The bot does not touch any other channel, it never renames one and it never
creates one.

## How the bot talks to Discord

The bot reads everything over the REST interface of Discord and uses the
gateway only for the events - the joins, the reactions and the commands. The
cache of the gateway is not used for a single lookup, it is not always filled
and it would only hide a mistake.

The bot also waits for the `Ready` event of the gateway before it registers the
commands, Discord refuses the registration before that.

## The commands

| Command | Who | What it does |
|---|---|---|
| `/link` | everybody | hands out the code that links the accounts |
| `/unlink` | everybody | removes the link again |
| `/profile` | everybody | shows the linked Minecraft account |
| `/server` | everybody | shows the name of the server |
| `/help` | everybody | lists the commands |
| `/announce` | staff | posts an announcement |
| `/release` | staff | posts a release |
| `/verify add` | staff | links a member by hand |
| `/verify remove` | staff | removes the link of a member |
| `/verify list` | staff | lists the linked members |
| `/reactionrole add` | staff | lets a reaction grant a role |
| `/reactionrole remove` | staff | stops a reaction from granting a role |
| `/reactionrole list` | staff | lists the reaction roles |

The staff commands need the role of `CROSSMOD_STAFF_ROLE_ID` and the bot needs
the permission to manage the roles.

## The HTTP endpoints

The mod and the build pipeline talk to the bot over HTTP. Every endpoint
except the health check needs the shared secret in the `X-Api-Key` header.

| Endpoint | What it does |
|---|---|
| `GET /health` | the health check of the host |
| `GET /api/status` | the state of the bot as JSON |
| `POST /api/link` | links a Minecraft account to the Discord account of the code |
| `POST /api/publish` | posts a release into the announcement channel |
| `POST /api/github` | posts a release from a GitHub webhook |

The body of `POST /api/link`:

```json
{ "code": "ABCD2345", "minecraftName": "Notch" }
```

The bot resolves the name against the official Mojang API, so only real
Minecraft accounts can be linked, and the account is linked to the Discord
account that asked for the code.

The body of `POST /api/publish`:

```json
{ "version": "0.2.0", "changelog": "What changed", "url": "https://..." }
```

## Announcing a release

There are two ways, both end up in the announcement channel.

**The build pipeline.** The workflow `.github/workflows/announce-release.yml`
posts a new release of the repository into the channel. It needs two secrets
of the repository:

* `CROSSMOD_BOT_URL` - the address of the bot, for example
  `https://crossmod-bot.up.railway.app`
* `CROSSMOD_API_KEY` - the shared secret of the bot

**A webhook of GitHub.** The endpoint `POST /api/github` answers the webhooks
of GitHub. Add a webhook of the type *Release* to the repository, point it at
`https://your-bot/api/github` and use a secret. When the secret is also set
as `CROSSMOD_GITHUB_SECRET` the bot checks the signature of the request.

**By hand.** The staff command `/release` posts a release as well.

## Deploying it for free

The bot is a plain container, so every host that runs Docker works. The
free tiers of Railway, Koyeb and Fly.io are enough for a bot of this size.

### With the command line of Railway

The command line of Railway uploads the folder it stands in, a repository on
GitHub is not needed.

```bash
cd bot
railway login
railway init
railway up
railway variables set CROSSMOD_DISCORD_TOKEN=... CROSSMOD_API_KEY=... CROSSMOD_GUILD_ID=...
railway domain
```

`railway up` finds the `Dockerfile` of the folder and builds the image. The
address that `railway domain` prints is the address the mod and the workflow
of the repository need.

The state of the bot is written into `/app/data`. Add a volume on that path in
the dashboard of the service, otherwise the links of the members are lost with
every deployment.

### With the dashboard of Railway

1. Put the repository on GitHub.
2. Create a project on [railway.app](https://railway.app) and add the
   repository.
3. Railway detects the `bot/Dockerfile`, point the build settings at it. When
   the whole repository is the context, change the `COPY` lines of the
   Dockerfile to `bot/CrossMod.Bot.csproj` and `bot/`.
4. Add the variables of the table above as variables of the service.
   `CROSSMOD_PORT` is set by Railway itself, the bot reads it.
5. Add a volume on `/app/data`, otherwise the links of the members are lost
   with every deployment.
6. Generate a domain for the service, that is the address the mod and the
   workflow of the repository need.

### Koyeb

1. Create a service from the repository on [koyeb.com](https://koyeb.com).
2. Set the build method to *Dockerfile* and the path to `bot/Dockerfile`.
3. Add the environment variables and a volume on `/app/data`.
4. Deploy and use the domain of the service.

## The client in the game

The mod sends the code of the player to the bot. The address of the bot and the
shared secret are read from the configuration file of the mod,
`config/crossmod.json`:

```json
{
  "discord": {
    "api_url": "https://crossmod-bot.up.railway.app",
    "api_key": "a-long-random-string"
  }
}
```

The flow is then:

1. The player runs `/link` in Discord and receives a code.
2. The player types `.link <code>` in the chat of the game.
3. The mod hands the code over to the bot, the bot links the accounts and
   grants the verified role.
