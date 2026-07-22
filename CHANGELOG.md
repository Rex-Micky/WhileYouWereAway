# Changelog

All notable changes to WhileYouWereAway are documented here.
This project adheres to [Semantic Versioning](https://semver.org/).

## [1.1.1] — 2026-07-22

### Fixed
- A sub-minute absence rendered as `While you were away (0m)`. Short absences now show
  seconds instead.

## [1.1.0]

Completes the original roadmap. Verified end-to-end with automated in-game tests.

### Added
- **Discord alerts** — a real-world ping when someone appears at your base while you're offline.
  Per-player webhooks via `/wywa discord <url>`, or a server-wide fallback. Requests are sent
  off the main thread, and mass-ping tokens in visitor names are defused.
- **Idle rewards** — a capped trickle accrues per hour offline and is granted on return.
- **Journal** — automatically records firsts (first diamond, first ancient debris, first nether
  star), boss kills (Ender Dragon, Wither, Warden), deaths and advancements. `/wywa journal`.
- **Reminders** — `/wywa remind <delay> <message>`, delivered live when due rather than only at
  login. Timed letters and notes to your future self now arrive live too.
- Journal added to the Home GUI.

### Security
- Only genuine `discord.com` webhook URLs are accepted. Players supply these themselves, so
  allowing arbitrary URLs would let someone use the server as an SSRF probe against internal
  addresses. Verified: `http://192.168.1.1/api/webhooks/1/x` is rejected.

## [1.0.0]

First feature-complete release. Verified end-to-end with automated in-game tests
(headless bots driving a real server) on Minecraft 1.20.6, 1.21.1, 1.21.4 and 1.21.11.

### Added
- **The Gazette** — the login recap rendered as a written newspaper; `/wywa chronicle` reopens it.
- **The Watcher** — a configurable guardian that lives at your base and hunts intruders while
  you're offline, scaling with your loyalty tier.
- **Loyalty** — the login streak becomes a visible shrine above your base with named tiers.
- **Guestbook & gifts** — visitors can sign your guestbook and leave items, delivered on your return.
- **Letters through time** — `/wywa letter` and `/wywa futureself` deliver messages in the future.
- **Return Tour** — a cinematic sweep of your base, with guaranteed gamemode/position restore.
- **Home GUI** — `/wywa` opens a hub linking every feature.
- Daily rewards with streak milestones, and a comeback bonus that scales with time away.
- Login recap: time away, in-game days passed, who was online (new players flagged),
  who came near your base, and pending mail.

### Fixed
- The Watcher never chased intruders: tameable mobs use owner-defence AI and ignore a manually
  set target. The default is now `IRON_GOLEM`, and the mob type is configurable.
- Loyalty shrines duplicated on every login when the tracked entity couldn't be resolved.

### Compatibility
- Attributes are now resolved reflectively, so the same jar works across the 1.21.3 Bukkit rename
  (`GENERIC_MAX_HEALTH` → `MAX_HEALTH`).
- `api-version` lowered to `1.20`, allowing the plugin to load on 1.20.6+.
