# WhileYouWereAway

WhileYouWereAway tells players what happened on the server while they were offline. If they were
gone for at least two minutes, they get a recap on login: how long they were away, how many in-game
days passed, who was online (new players are marked), who came near their base and any mail. The
recap also opens as a written book called The Gazette. It is on
[SpigotMC](https://www.spigotmc.org/resources/137320/).

## Features

- Base and Watcher: players set a base with `/wywa setbase`. Anyone within 24 blocks of it is
  logged as a visitor while the owner is offline. `/wywa watcher` puts an iron golem at the base
  that attacks other players within 16 blocks while the owner is offline. It gets stronger with the
  owner's loyalty tier.
- Daily rewards and streaks: `/wywa daily` pays out once a day, with bonuses at 3, 7 and 30 days.
  Players coming back after 2, 7 or 30 days get a welcome-back reward.
- Loyalty: the daily reward streak sets a tier from Newcomer to Legend, shown as floating text at
  the base.
- Guestbook and gifts: visitors can sign an offline player's guestbook at their base. The item in
  your hand can be sent to any player as a gift, and offline players get it when they log in.
- Mail, delayed letters, notes to your future self and reminders.
- A journal of firsts (diamond, ancient debris, nether star), boss kills, deaths and advancements.
- Idle rewards: XP and items for every hour offline, up to 8 hours.
- Discord alerts for base visitors, through a server webhook or a player's own. Only Discord
  webhook URLs are accepted.
- `/wywa tour` flies the player around their base in spectator mode and puts them back afterwards.

Rewards can be XP, items or console commands, so you can hand out money or crate keys through
other plugins. All of it, and every message, is set in `config.yml`.

## Commands

```
/wywa                                   open the menu
/wywa chronicle | journal | guestbook   reread the Gazette, the journal or the guestbook
/wywa daily | streak                    claim the daily reward or show the streak
/wywa setbase | base | watcher | tour   set or show the base, summon the Watcher, take the tour
/wywa sign <message>                    sign the guestbook of the base you're at
/wywa gift <player>                     send the item in your hand
/wywa mail <player> <message>           send mail, delivered now or on their next login
/wywa letter <player> <delay> <message> send a letter that arrives later, e.g. 3d
/wywa futureself <delay> <message>      leave yourself a note
/wywa remind <delay> <message>          set a reminder, e.g. 2h feed the animals
/wywa discord <url> | off               send your base alerts to your own webhook
/wywa reload                            reload config.yml
```

`/away` works too. Everything except `reload` is open to all players; `reload` needs `wywa.admin`.

## Notes

The Watcher defaults to `IRON_GOLEM`. Wolves and cats get tamed to the owner, and tamed mobs ignore
the target the plugin gives them, so they would just sit there.

Discord alerts stay off until you set `discord.enabled` to true. A webhook URL typed with
`/wywa discord` shows up in the console log like any other command, so the server-wide webhook in
the config is the safer option.

## Installing and building

Put the jar from the [releases page](https://github.com/Rex-Micky/WhileYouWereAway/releases) in
`plugins/` and restart. It needs Spigot or Paper 1.20.6 to 1.21.x on Java 21. I have tested it
in-game on 1.20.6, 1.21.1, 1.21.4 and 1.21.11. To build it, run `mvn clean package` with JDK 21.

Questions and bug reports: [Discord](https://discord.gg/pfF5TpQ9UE) or the issue tracker here.
MIT licensed, see [LICENSE](LICENSE).
