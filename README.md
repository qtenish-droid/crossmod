# CrossMod

CrossMod is an all in one PvP client for Minecraft. The current build is the
visual shell: a floating sheet of clear smoked glass with a hot pink accent, a
labeled sidebar, a centred search and filter header and an empty two-column
module grid.

The project is a client only Fabric mod for **Minecraft 26.3**.

![The CrossMod interface over a blurred world](docs/screenshots/interface.png)

*The shell floats over the blurred world with breathing room around every edge.
The search and filters sit in the centred header. The world behind it is
captured, blurred and darkened with a neutral scrim, independently of the
vanilla menu blur setting. The HUD is hidden while the shell is open.*

---

## Features

* **A floating glass shell** - the rounded panel is scaled down as a whole and
  centred, leaving clear breathing room between the interface and every edge of
  the screen. Every surface is drawn from anti-aliased four-times textures, so
  the corners stay smooth at any GUI scale.
* **A captured, heavily blurred backdrop** - the last world frame is downscaled,
  box-blurred and drawn as an opaque first layer. A neutral dark scrim is applied
  on top, so the glass stays readable without taking on a red cast and never
  depends on the user's *Menu Background Blurriness* setting.
* **A centred header** - the search field sits in the upper middle of the panel,
  the filter pills are centred underneath it and the close button sits in the
  top-right corner. The sidebar starts below the header.
* **Two proportional card columns** - the grid is ready for registered modules
  and scrolls inside the panel. It is intentionally empty in this build.
* **A labeled sidebar** - Modules, Settings, Social, Wardrobe and Profiles, with
  Edit HUD and the Discord link pinned to the bottom.
* **A clear search and filter header** - search by name, then narrow the list
  with the All, PvP, HUD, Visuals and Player filters, each with its own count.
* **Clear card states** - the icon, border and toggle turn hot pink when a card
  is on and stay gray when it is off.
* **Configurable key bindings** - **Right Shift** opens and closes the shell by
  default. It is registered with Minecraft and can be changed in the controls
  screen.

The module registry ships empty on purpose. The preview catalogue has been
removed; new modules can be bound to the card list as they are implemented.

## Requirements

| | |
|---|---|
| Minecraft | 26.3 |
| Fabric Loader | 0.19.3 or newer (tested up to 0.20.1) |
| Fabric API | 0.164.0+26.3 |
| Java | 25 |

## Building

The project uses the Gradle wrapper that ships with the repository, so no local
Gradle installation is needed.

```bash
./gradlew build
```

The finished mod is written to `build/libs/`.

To start a development client:

```bash
./gradlew runClient
```

> **Note**
> Minecraft 26.3 and Fabric Loom require a Java 25 runtime. The Gradle daemon
> itself has to run on Java 25 as well. The `settings.gradle` file registers the
> Foojay toolchain resolver, so a matching JDK is downloaded automatically when
> one is missing.

## Project layout

```
src/main/java/io/github/crossmod
├── CrossMod.java                  Common entry point and configuration store
└── config/ConfigManager.java      JSON backed configuration

src/client/java/io/github/crossmod/client
├── CrossModClient.java            Client entry point, wiring of all services
├── keybind/KeyBinds.java          Key bindings of the mod
├── module/                        The module framework
│   ├── Module.java                Base class of every module
│   ├── ModuleCategory.java        Categories used by the module framework
│   └── ModuleManager.java         Registry, life cycle and persistence
├── setting/                       The settings of the modules
├── discord/                       The link between the game and the Discord server
│   ├── DiscordLinkClient.java     Talks to the bot of the server
│   └── DiscordChatCommands.java   The .link command of the chat
├── gui/                           The interface
│   ├── screen/                    The floating shell and the placeholder pages
│   ├── render/                    Drawing helpers and the captured menu backdrop
│   └── theme/                     Colours, metrics and user settings

bot/                              The Discord bot of the server (C# / .NET 8)
```

### Adding a module

1. Create the module in `module/impl/<category>/` and extend `Module`.
2. Register it in `ModuleManager#registerBuiltIns`.

```java
public final class ExampleModule extends Module {

    private final BooleanSetting enabled = addSetting(new BooleanSetting(
            "enabled", "Enabled", "Turns the example on", true));

    public ExampleModule() {
        super("example", "Example", "An example module", ModuleCategory.UTIL);
    }

    @Override
    public void onTick() {
        // Runs every client tick while the module is active.
    }
}
```

The module is stored in `config/crossmod.json`. The preview catalogue has been
removed from the shell; binding registered modules to the card grid is the next
step as new modules are added.

## The Discord bot

The server has its own bot, it lives in [`bot/`](bot/README.md) and is written
in C# on .NET 8. It welcomes the new members, hands out roles on reactions and
links a Discord account to a Minecraft account.

The mod and the bot talk to each other over a small HTTP interface. A player
runs `/link` in Discord, types `.link <code>` in the chat of the game and the
bot grants the verified role.

The address of the bot and the shared secret are read from the configuration
file of the mod, `config/crossmod.json`:

```json
{
  "discord": {
    "api_url": "http://localhost:8080",
    "api_key": "a-long-random-string"
  }
}
```

While the bot runs on the same machine, `http://localhost:8080` is the
address. A public address comes with the deployment, see
[`bot/README.md`](bot/README.md).

## The interface

### Layout

```
┌───────────────────────────────────────────────────────────────┐
│                  Search anything...                     [X]   │
│            [All] [PvP] [HUD] [Visuals] [Player]               │
├─────────────┬─────────────────────────────────────────────────┤
│  Modules    │                                                 │
│  Settings   │                 No modules yet                 │
│  Social     │          New modules will appear here.          │
│  Wardrobe   │                                                 │
│  Profiles   │                                                 │
│             │                                                 │
│  Edit HUD   │                                                 │
│  Discord    │                                                 │
└─────────────┴─────────────────────────────────────────────────┘
```

* The whole interface is scaled to 80% and centred, so the panel keeps a clear
  gap between itself and every window edge. The panel has a fixed 108 unit
  sidebar, an 86 unit header and a two-column content area.
* The full-width header holds the 300 unit search field in the upper middle,
  with the filter pills centred below it. The close button sits in the top-right
  corner. The old top-left logo has been removed.
* The sidebar navigation starts below the header. The sidebar pages are
  placeholders in this shell; the Modules page shows the empty state until new
  modules are bound to the grid.
* Every card is 49 units high with a 34 unit icon tile. The two columns share
  the remaining width equally with an 8 unit gutter. The grid scrolls with the
  mouse wheel when there are more cards than fit.
* Clicking a filter or typing in the search field rebuilds the visible list.
  The filter counts are zero while the registry is empty.

### The backdrop

A CrossMod screen does not draw the opaque vanilla menu background. Instead it:

1. captures the current world frame when the screen opens,
2. downscales it to 160 pixels wide and box-blurs it twice,
3. draws the blurred texture opaquely over the whole viewport, so no earlier
   frame or UI can accumulate behind the glass,
4. applies a neutral dark scrim, independent of the vanilla blur option, so the
   glass keeps its clarity without a red cast, and
5. draws the scaled panel, header, cards and text on top.

The HUD is hidden while the shell is open. The captured frame is refreshed every
time the shell is opened, so it always reflects the current world.

### Assets

All surfaces and icons are generated procedurally, so every corner and every
glyph can be reviewed and reproduced:

```bash
javac -d tools/asset-gen/out tools/asset-gen/GuiSkinGenerator.java
java -cp tools/asset-gen/out tools.GuiSkinGenerator src/main/resources/assets/crossmod/textures/gui

javac -d tools/asset-gen/out tools/asset-gen/IconGenerator.java
java -cp tools/asset-gen/out tools.IconGenerator src/main/resources/assets/crossmod/textures/gui
```

The generators write four-times resolution textures; the game scales them down
with linear filtering. The generated files live in
`src/main/resources/assets/crossmod/textures/gui`.

### Developer hooks

The interface can be verified without a human at the keyboard. The following
environment variables are read by the client and are inert otherwise:

| Variable | Effect |
|---|---|
| `CROSSMOD_DEV_OPEN_GUI` | Opens the interface after the given amount of ticks. |
| `CROSSMOD_DEV_OPEN_MODULE_SETTINGS` | Opens the settings of the first module. |
| `CROSSMOD_DEV_OPEN_GUI_SETTINGS` | Opens the settings of the interface. |
| `CROSSMOD_DEV_SCREENSHOT` | Takes a screenshot of the game into `run/screenshots`. |

```bash
CROSSMOD_DEV_OPEN_GUI=90 CROSSMOD_DEV_SCREENSHOT=1 ./gradlew runClient
```

For automated screenshots the client also stages the developer world: it sets
the time to day, clears the weather and moves the player high above the world in
spectator mode, so the captured backdrop has a bright sky instead of a cave.

## Links

* Discord: https://discord.gg/tDe4cZYyuV
