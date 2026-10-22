# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/)
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.1.0] - 2026-10-21

### Added

* Truly translucent glass interface: the blurred game stays visible through the
  panel, the cards and the settings screens.
* A single sheet of glass with an integrated icon rail and three equal module
  columns for PvP, Perf and Util.
* Resolution independent interface that ignores the GUI scale of Minecraft and
  always renders on whole pixels.
* Module framework with a life cycle, settings, key bindings and a JSON backed
  configuration. The built in module registry starts empty.
* Settings screens for the interface and for every module.
* Module list with a search field, tooltips and per column scrolling.
* The Discord bot of the server in `bot/`: welcome messages, a member counter,
  reaction roles, account linking, announcements and release posts.
* The `.link <code>` chat command that links the Minecraft account of the
  player to the Discord account of the player.
