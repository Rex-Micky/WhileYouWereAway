package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Builds and sends the "while you were away" digest when a player logs in. */
public class RecapService {

    private final WhileYouWereAwayPlugin plugin;

    public RecapService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    public void deliver(Player player) {
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        d.setName(player.getName());

        // First time we've ever seen this player leave - just welcome them.
        if (d.getLastLogout() == 0) {
            plugin.send(player, "welcome-first", "player", player.getName());
            d.clearPending();
            return;
        }

        long away = System.currentTimeMillis() - d.getLastLogout();
        long minAway = plugin.getConfig().getLong("min-away-seconds", 120) * 1000L;
        if (away < minAway) {          // quick relog - don't nag
            d.clearPending();
            return;
        }

        plugin.send(player, "header", "duration", formatDuration(away));
        boolean any = false;

        // Welcome-back reward scaling with how long they were gone.
        if (plugin.getComeback().grantIfEligible(player, away)) any = true;

        if (plugin.getConfig().getBoolean("recap.show-days-passed", true)) {
            long days = daysPassed(d);
            if (days > 0) {
                plugin.sendRaw(player, "days", "days", days);
                any = true;
            }
        }

        if (plugin.getConfig().getBoolean("recap.show-online-players", true) && !d.getJoinedWhileAway().isEmpty()) {
            int max = plugin.getConfig().getInt("recap.max-names", 10);
            List<String> names = new ArrayList<>();
            for (String n : d.getJoinedWhileAway()) {
                String label = "&f" + n;
                if (d.getNewPlayersWhileAway().contains(n)) label += plugin.format("new-tag");
                names.add(label);
                if (names.size() >= max) break;
            }
            plugin.sendRaw(player, "online",
                    "count", d.getJoinedWhileAway().size(),
                    "names", String.join("&7, ", names));
            any = true;
        }

        if (plugin.getConfig().getBoolean("recap.show-base-visits", true) && !d.getBaseVisits().isEmpty()) {
            for (Map.Entry<String, Integer> e : d.getBaseVisits().entrySet()) {
                String count = e.getValue() > 1 ? " &8(" + e.getValue() + "x)" : "";
                plugin.sendRaw(player, "visits", "visitor", e.getKey(), "count", count);
            }
            any = true;
        }

        List<PlayerData.Mail> dueMail = new ArrayList<>();
        if (plugin.getConfig().getBoolean("recap.show-mail", true)) {
            for (PlayerData.Mail m : d.getMail()) if (m.isDue()) dueMail.add(m);
            if (!dueMail.isEmpty()) {
                plugin.sendRaw(player, "mail-header");
                for (PlayerData.Mail m : dueMail) {
                    plugin.sendRaw(player, "mail-line", "from", m.from, "message", m.message);
                }
                any = true;
            }
        }

        if (!any) plugin.sendRaw(player, "quiet");
        plugin.getChronicle().generate(player, d, away);
        d.getMail().removeAll(dueMail);   // delivered letters go; future-dated ones stay
        d.clearPending();
    }

    private long daysPassed(PlayerData d) {
        if (Bukkit.getWorlds().isEmpty()) return 0;
        long delta = Bukkit.getWorlds().get(0).getFullTime() - d.getWorldTimeAtQuit();
        return delta > 0 ? delta / 24000L : 0;
    }

    /** e.g. 2d 3h 40m, or plain seconds for a short absence so it never reads "0m". */
    public static String formatDuration(long millis) {
        long total = Math.max(0, millis / 1000);
        if (total < 60) return total + "s";

        long s = total;
        long d = s / 86400; s %= 86400;
        long h = s / 3600;  s %= 3600;
        long m = s / 60;
        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m");
        return sb.toString().trim();
    }
}
