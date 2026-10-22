# Putting the bot online for good

The bot runs on your machine right now (`cd bot && dotnet run`). It goes
offline when the terminal closes. This file is the checklist for the version
that runs on a server, 24 hours a day.

Everything that is already done is marked with a check.

- [x] The token, the shared secret and every id are in `bot/local.env`
- [x] The bot is registered in the server, the commands show up right away
- [x] The roles and the channels are created
- [x] The `Dockerfile` is in `bot/`
- [x] The image builds on a server (it is the same build as the local one)
- [x] The mod is pointed at the bot, `config/crossmod.json` in every launcher

## 1. The repository on GitHub

The hosts build from a repository. The local repository is already prepared,
only the remote is missing.

```bash
cd C:\Users\tenis\OneDrive\Pictures\white-vision\CrossMod
git remote add origin https://github.com/<your-name>/crossmod.git
git push -u origin main
```

Git for Windows is installed at `C:\Users\tenis\tools\mingit\cmd` and is on
the path of the user. A new terminal finds `git` without any further work.

## 2. The host

### Railway, with the command line

The command line of Railway is installed at
`C:\Users\tenis\tools\railway\railway.exe` and is signed in.

```bash
cd bot
railway init --name crossmod-bot
railway up --yes --detach
```

The workspace of a new account allows one project every 30 seconds. If it
answers with *You are creating projects too quickly*, wait a minute and try
again, and do not run it twice in a row.

The variables of the service come out of `local.env`, the two secrets are
piped through the standard input so they never end up in a command line:

```bash
railway variable set CROSSMOD_DISCORD_TOKEN --stdin --service CrossMod --skip-deploys
railway variable set CROSSMOD_API_KEY --stdin --service CrossMod --skip-deploys
railway variable set CROSSMOD_GUILD_ID=1558722727542657134 --service CrossMod --skip-deploys
railway variable set CROSSMOD_WELCOME_CHANNEL_ID=1562664187140907152 --service CrossMod --skip-deploys
railway variable set CROSSMOD_MEMBER_COUNT_CHANNEL_ID=1562664625122705479 --service CrossMod --skip-deploys
railway variable set CROSSMOD_ANNOUNCEMENT_CHANNEL_ID=1562664191318429717 --service CrossMod --skip-deploys
railway variable set CROSSMOD_VERIFIED_ROLE_ID=1562655759479738388 --service CrossMod --skip-deploys
railway variable set CROSSMOD_STAFF_ROLE_ID=1561282905743954000 --service CrossMod --skip-deploys
railway variable set CROSSMOD_PORT=8080 --service CrossMod --skip-deploys

railway volume add --service CrossMod --mount-path /app/data
railway redeploy --yes --service CrossMod
railway domain --service CrossMod --port 8080
```

The address that the last command prints is the address of the bot.

### Koyeb

Koyeb builds from a repository, the `Dockerfile` is the path `bot/Dockerfile`.
The variables of the service are the same as above, a volume on `/app/data`
keeps the links of the members.

## 3. The mod points at the public address

The address of the service replaces `localhost` in the configuration file of
the mod, in every launcher:

```
C:\Users\tenis\AppData\Roaming\.minecraft\config\crossmod.json
C:\Users\tenis\AppData\Roaming\ModrinthApp\profiles\Fabric 26.3\config\crossmod.json
C:\Users\tenis\AppData\Roaming\ModrinthApp\profiles\Fabric 1.21.11\config\crossmod.json
```

```json
{
  "discord": {
    "api_url": "https://crossmod-bot.up.railway.app",
    "api_key": "the shared secret"
  }
}
```

## 4. A new token

The token that is in `local.env` was written into a chat once and is
therefore treated as public. Before the bot goes online it should be replaced:

1. Developer portal → the application → **Bot** → **Reset Token**.
2. The new token goes into `local.env` and into the variables of the service.
3. The old token is dead from that moment on, the bot reconnects by itself.

## 5. The releases reach the channel

The workflow `.github/workflows/announce-release.yml` posts a new release of
the repository into the channel of the bot. It needs two secrets of the
repository, under **Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `CROSSMOD_BOT_URL` | the address of the service |
| `CROSSMOD_API_KEY` | the shared secret of the bot |

A release of the repository then shows up in the channel without anybody
doing anything.
