# WhileYouWereAway

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.6--1.21.x-brightgreen)
![Platform](https://img.shields.io/badge/platform-Spigot%20%7C%20Paper-orange)
![Java](https://img.shields.io/badge/Java-21%2B-blue)
![Version](https://img.shields.io/badge/version-1.0.0-informational)
![License](https://img.shields.io/badge/license-MIT-lightgrey)

> Your server keeps living while you're offline — and coming back should feel like something.

Most plugins treat logging in as a non-event. **WhileYouWereAway** makes your *absence* a first-class
part of the game: the world notices you were gone, remembers what happened, guards your home, and
rewards you for coming back.

Log in and you get a **personal newspaper** of everything you missed, a **guardian** that tells you who
came prowling around your base, **gifts and guestbook notes** left by other players, **letters that
travelled through time** to reach you, and a **loyalty shrine** that grows the longer you keep returning.

---

## Table of contents

- [Features](#features)
- [Installation](#installation)
- [Commands](#commands)
- [Permissions](#permissions)
- [Configuration](#configuration)
- [Compatibility](#compatibility)
- [Building from source](#building-from-source)
- [Roadmap](#roadmap)
- [Support](#support)
- [License](#license)

---

## Features

### 📰 The Gazette
Your recap isn't a wall of chat lines — it's a **written newspaper** that opens on login, with headlines
for everything you missed. Reread any edition with `/wywa chronicle`.

### 🛡️ The Watcher
Summon a **guardian** that lives at your base. While you're offline it **hunts down intruders**, and it
grows stronger with your loyalty tier. Fully configurable mob type, health, damage and guard range.

### 🌳 Loyalty
Your login streak becomes a visible **shrine floating above your base**, showing your tier —
Newcomer → Regular → Devoted → Loyal → Legend. Progression you can actually see and show off.

### 📖 Guestbook & gifts
Visitors to your base can **sign your guestbook** and **leave you gifts**, which are waiting when you
return. Your absence becomes asynchronous multiplayer.

### ⏳ Letters through time
Send a letter that arrives **days from now**, or leave a **note for your future self**. It waits until
it's due, then finds you on login.

### 🎁 Daily rewards, streaks & comeback bonus
Escalating daily rewards with streak milestones, plus a **welcome-back bonus that scales with how long
you were gone** — the nudge that pulls lapsed players back. Rewards are items, XP or console commands,
so you can hand out money, ranks or crate keys through any plugin you already run.

### 🎬 Return Tour
A cinematic camera sweep around your home when you come back. Your gamemode and exact position are
always restored — even if you disconnect mid-tour.

### 🗂️ Home GUI
One clickable menu (`/wywa`) tying it all together — Gazette, daily reward, loyalty, guestbook,
Watcher, base and tour.

Plus: who logged on while you were away (new players flagged), who came near your base, how many
in-game days passed, and every message fully configurable.

## Installation

1. Download `WhileYouWereAway-1.0.0.jar`.
2. Drop it into your server's `plugins/` folder.
3. Restart the server. A `config.yml` is generated on first run.

No dependencies required.

## Commands

Base command: `/wywa` — alias `/away`. Running it with no arguments opens the Home GUI.

| Command | Description |
|---|---|
| `/wywa` | Open the Home hub |
| `/wywa chronicle` | Reopen your latest Gazette |
| `/wywa daily` · `/wywa streak` | Claim your daily reward · view your streak |
| `/wywa setbase` · `/wywa base` | Set the base that's watched · show it |
| `/wywa watcher` | Summon your Watcher to your base |
| `/wywa guestbook` | Read who signed your guestbook |
| `/wywa sign <message>` | Sign the guestbook of the base you're standing at |
| `/wywa gift <player>` | Leave the held item as a gift |
| `/wywa mail <player> <message>` | Send mail (instant if online, else on their return) |
| `/wywa letter <player> <delay> <message>` | A letter that arrives later, e.g. `3d` |
| `/wywa futureself <delay> <message>` | Leave a note for your future self |
| `/wywa tour` | Take the cinematic tour of your base |
| `/wywa reload` | (admin) Reload the configuration |

## Permissions

| Permission | Description | Default |
|---|---|---|
| `wywa.use` | All player features | everyone |
| `wywa.admin` | Reload the configuration | op |

## Configuration

Everything is editable in `config.yml` and applied with `/wywa reload`.

```yaml
min-away-seconds: 120         # skip the recap for quick relogs
base:
  radius: 24                  # how close counts as "near your base"
watcher:
  mob-type: IRON_GOLEM        # tameable mobs (WOLF/CAT) will NOT chase intruders — see below
  invulnerable: true
  guard-range: 16
loyalty:
  shrine: true
  tiers: [ { at: 0, name: "&7Newcomer" }, { at: 7, name: "&bDevoted" }, ... ]
daily-rewards:
  reward: { xp: 20, items: ["DIAMOND:1"], commands: [] }
  milestones: { 3: {...}, 7: {...}, 30: {...} }
comeback:
  tiers: { 2: {...}, 7: {...}, 30: {...} }      # keyed by days away
chronicle:
  auto-open: true
```

> **On `watcher.mob-type`:** it defaults to `IRON_GOLEM` for a reason. Tameable mobs (`WOLF`, `CAT`)
> use owner-defence AI and will **ignore a manually set target**, so they sit still while an intruder
> walks past. Use them only if you want a purely decorative companion.

## Compatibility

Requires **Java 21+** (the runtime Minecraft itself requires from 1.20.5 onward).

Automatically verified by booting a real server per version and exercising the plugin in-game on:
**1.20.6**, **1.21.1**, **1.21.4** and **1.21.11** — all loading cleanly with no errors. Versions
between those are expected to work but have not been individually tested.

## Building from source

Requires **JDK 21+** and **Maven**.

```bash
mvn clean package
# → target/WhileYouWereAway-1.0.0.jar
```

## Roadmap

- 📱 Discord webhook alerts when someone breaks into your base while you're offline.
- ⏳ Idle rewards that accrue while you're away.
- 📚 A personal journal of your milestones and firsts.
- ⏰ Reminders delivered on your next login.

## Support

- 🌐 Website: [studio.rexmicky.dev](https://studio.rexmicky.dev)
- 💬 Discord: [discord.gg/pfF5TpQ9UE](https://discord.gg/pfF5TpQ9UE)

## License

Released under the [MIT License](LICENSE) — © 2026 Rex Micky · Rex's Studio.
