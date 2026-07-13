# WhileYouWereAway

![Minecraft](https://img.shields.io/badge/Minecraft-1.21.x-brightgreen)
![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-orange)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![Version](https://img.shields.io/badge/version-1.0.0-informational)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

> Log in and get a personalized recap of everything that happened while you were offline.

**WhileYouWereAway** turns logging in into a moment. The second a player joins, they get a short,
personal digest of what they missed: how long they were gone, how much in-game time passed, who was on
(with brand-new players flagged), **who wandered near their base**, and any **offline mail** other
players left them. It's a small touch that makes a server feel alive and keeps players coming back to
see what they missed — a genuine retention tool, with no dependencies.

---

## Table of contents

- [Features](#features)
- [How the recap works](#how-the-recap-works)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Building from source](#building-from-source)
- [Roadmap](#roadmap)
- [Support](#support)
- [License](#license)

---

## Features

- ⏱️ **Time away** — how long you were offline, and how many in-game days passed.
- 👥 **Who was on** — the players who logged in while you were gone, with **new players flagged**.
- 📍 **Base watch** — find out who came near your base while you were offline (the personal "trail cam").
- ✉️ **Offline mail** — leave a message for a player; they get it in their recap next time they log in.
- 🏠 **Simple base setup** — one command to mark the spot you want watched.
- ⚙️ **Configurable** — toggle each recap section, tune the base radius, and edit every message.
- 💾 **Persistent** across restarts, lightweight, and dependency-free.
- 🧩 Works on **Spigot and Paper 1.21.x**.

## How the recap works

- When you **log out**, the plugin remembers the time and (once) your base — your bed spawn, or where
  you logged off if you have no bed. You can override it any time with `/wywa setbase`.
- While you're **offline**, it quietly records who logs in and who comes within range of your base.
- When you **log back in** (after a configurable minimum time away), it delivers your digest and clears
  it, ready to start fresh.

## Installation

1. Download `WhileYouWereAway-1.0.0.jar`.
2. Drop it into your server's `plugins/` folder.
3. Restart the server. A `config.yml` is generated on first run.

## Commands

Base command: `/wywa` — alias `/away`.

| Command | Description |
|---|---|
| `/wywa setbase` | Set the location watched for visitors to where you're standing |
| `/wywa base` | Show your current base location |
| `/wywa mail <player> <message>` | Send a message — delivered now if they're online, or in their next recap |
| `/wywa reload` | (admin) Reload the configuration |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `wywa.use` | Set a base and send mail | everyone |
| `wywa.admin` | Reload the configuration | op |

## Configuration

```yaml
min-away-seconds: 120         # skip the recap for quick relogs shorter than this
base:
  radius: 24                  # how close a player must be to count as "near your base"
  visit-cooldown-seconds: 120 # don't recount the same visitor more often than this
recap:
  show-days-passed: true
  show-online-players: true
  show-base-visits: true
  show-mail: true
  max-names: 10               # cap how many player names are listed
messages:
  # every player-facing string is here and supports & colour codes + {placeholders}
```

Edit anything and run `/wywa reload`.

## Building from source

Requires **JDK 21+** and **Maven**.

```bash
mvn clean package
# → target/WhileYouWereAway-1.0.0.jar
```

Only standard Bukkit APIs are used, so the built jar runs on both Spigot and Paper.

## Roadmap

- 🌾 Report crop growth, breeding and furnace output finished while you were away.
- 💰 Market / shop price shifts (with an economy plugin installed).
- 🔔 A summary of deaths and notable events near your base.
- 📜 A scrollable GUI history instead of chat lines.

## Support

- 🌐 Website: [studio.rexmicky.dev](https://studio.rexmicky.dev)
- 💬 Discord: [discord.gg/pfF5TpQ9UE](https://discord.gg/pfF5TpQ9UE)

## License

Released under the [MIT License](LICENSE) — © 2026 Rex Micky · Rex's Studio.
