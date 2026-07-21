# Changelog

All notable changes to WhileYouWereAway are documented here.
This project adheres to [Semantic Versioning](https://semver.org/).

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
