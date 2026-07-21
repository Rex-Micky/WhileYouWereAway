package dev.rexmicky.wywa;

import org.bukkit.entity.Player;

import java.util.List;

/** The player's own story — firsts and milestones, written automatically as they happen. */
public class JournalService {

    private final WhileYouWereAwayPlugin plugin;

    public JournalService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("journal.enabled", true);
    }

    public void record(Player player, String category, String text) {
        if (!enabled()) return;
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        d.setName(player.getName());
        d.addJournalEntry(category, text, plugin.getConfig().getInt("journal.max-entries", 100));
    }

    /** Record a milestone only if it hasn't been written before — for genuine "firsts". */
    public void recordOnce(Player player, String category, String text) {
        if (!enabled()) return;
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        for (PlayerData.Mail entry : d.getJournal()) {
            if (entry.message.equals(text)) return;
        }
        record(player, category, text);
    }

    public void view(Player player) {
        if (!enabled()) { plugin.send(player, "journal-disabled"); return; }
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        List<PlayerData.Mail> entries = d.getJournal();
        if (entries.isEmpty()) { plugin.send(player, "journal-empty"); return; }

        int show = plugin.getConfig().getInt("journal.show-entries", 12);
        plugin.send(player, "journal-header", "count", entries.size());
        int start = Math.max(0, entries.size() - show);
        for (int i = start; i < entries.size(); i++) {
            PlayerData.Mail e = entries.get(i);
            plugin.sendRaw(player, "journal-entry",
                    "category", e.from, "text", e.message, "ago", ago(e.time));
        }
    }

    private String ago(long time) {
        long secs = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        if (secs < 60) return secs + "s ago";
        if (secs < 3600) return (secs / 60) + "m ago";
        if (secs < 86400) return (secs / 3600) + "h ago";
        return (secs / 86400) + "d ago";
    }
}
