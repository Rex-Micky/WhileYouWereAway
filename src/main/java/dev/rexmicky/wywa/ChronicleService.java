package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * "The Gazette" - renders a player's while-you-were-away recap as a written
 * newspaper (headlines, not chat lines) and opens it on login.
 */
public class ChronicleService {

    private final WhileYouWereAwayPlugin plugin;
    private final Map<UUID, ItemStack> latest = new HashMap<>();

    public ChronicleService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    /** Build this player's edition from their pending recap and (optionally) open it. */
    public void generate(Player player, PlayerData d, long awayMillis) {
        if (!plugin.getConfig().getBoolean("chronicle.enabled", true)) return;

        String masthead = plugin.getConfig().getString("chronicle.server-name", "The Gazette");
        List<String> pages = new ArrayList<>();

        pages.add("§0§l" + masthead + "\n§8§m>>>>>>>>>>>>>>>\n§7While you were\n§7away for\n§0§l"
                + RecapService.formatDuration(awayMillis) + "\n§8§m>>>>>>>>>>>>>>>\n§8§oRead on...");

        List<String> stories = new ArrayList<>();

        long days = daysPassed(d);
        if (days > 0) {
            stories.add("§0§lTIME MARCHES ON\n§8§m------\n§7" + days + " in-game day(s)\npassed while you\nwere gone.");
        }
        if (!d.getJoinedWhileAway().isEmpty()) {
            stories.add("§0§lA BUSY REALM\n§8§m------\n§7" + d.getJoinedWhileAway().size()
                    + " soul(s) walked\nthese lands while\nyou slept:\n§8" + names(d));
        }
        for (Map.Entry<String, Integer> e : d.getBaseVisits().entrySet()) {
            String times = e.getValue() > 1 ? " §8(" + e.getValue() + "x)" : "";
            stories.add("§4§lTRESPASS!\n§8§m------\n§7" + e.getKey() + " was seen\nlurking near your\nbase" + times + ".");
        }
        long dueMail = d.getMail().stream().filter(PlayerData.Mail::isDue).count();
        if (dueMail > 0) {
            stories.add("§0§lWORD ARRIVES\n§8§m------\n§7" + dueMail
                    + " letter(s) await\nyou. Open them\nwith §8/wywa mail§7.");
        }
        if (stories.isEmpty()) {
            stories.add("§0§lALL QUIET\n§8§m------\n§7Nothing stirred\nwhile you were\ngone. Rest easy.");
        }
        pages.addAll(stories);

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        if (meta != null) {
            meta.setTitle(ChatColor.stripColor(masthead));
            meta.setAuthor("The Gazette");
            meta.setPages(pages);
            book.setItemMeta(meta);
        }

        latest.put(player.getUniqueId(), book);
        if (plugin.getConfig().getBoolean("chronicle.auto-open", true)) {
            player.openBook(book);
        } else {
            plugin.send(player, "chronicle-ready");
        }
    }

    /** Reopen the player's most recent edition (via /wywa chronicle). */
    public void reopen(Player player) {
        ItemStack book = latest.get(player.getUniqueId());
        if (book == null) { plugin.send(player, "chronicle-none"); return; }
        player.openBook(book);
    }

    private String names(PlayerData d) {
        int max = plugin.getConfig().getInt("recap.max-names", 10);
        List<String> out = new ArrayList<>();
        for (String n : d.getJoinedWhileAway()) {
            out.add(d.getNewPlayersWhileAway().contains(n) ? n + " (new)" : n);
            if (out.size() >= max) break;
        }
        return String.join(", ", out);
    }

    private long daysPassed(PlayerData d) {
        if (Bukkit.getWorlds().isEmpty()) return 0;
        long delta = Bukkit.getWorlds().get(0).getFullTime() - d.getWorldTimeAtQuit();
        return delta > 0 ? delta / 24000L : 0;
    }
}
