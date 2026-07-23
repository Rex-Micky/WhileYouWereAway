package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;

/** The "Home" hub - one clickable menu linking every While-You-Were-Away feature. */
public class HomeGui implements Listener {

    private static final class HomeHolder implements InventoryHolder {
        private Inventory inv;
        @Override public Inventory getInventory() { return inv; }
    }

    private final WhileYouWereAwayPlugin plugin;

    public HomeGui(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        HomeHolder holder = new HomeHolder();
        Inventory inv = Bukkit.createInventory(holder, 27,
                ChatColor.translateAlternateColorCodes('&', "&8While You Were Away"));
        holder.inv = inv;

        PlayerData d = plugin.getStore().get(player.getUniqueId());
        inv.setItem(10, icon(Material.WRITTEN_BOOK, "&bThe Gazette", "&7Reread your latest recap"));
        inv.setItem(11, icon(Material.NETHER_STAR, "&eDaily Reward", "&7Claim today's reward", "&7Streak: &f" + d.getCurrentStreak()));
        inv.setItem(12, icon(Material.EXPERIENCE_BOTTLE, "&aLoyalty", "&7" + plugin.getLoyalty().tierName(d.getCurrentStreak()), "&7Best streak: &f" + d.getLongestStreak()));
        inv.setItem(13, icon(Material.WRITABLE_BOOK, "&dGuestbook", "&7See who signed your base", "&7Entries: &f" + d.getGuestbook().size()));
        inv.setItem(14, icon(Material.BONE, "&6The Watcher", "&7Summon your guardian", "&7to your base"));
        inv.setItem(15, icon(Material.COMPASS, "&fYour Base", d.hasBase() ? "&7" + d.getBaseWorld() + " " + (int) d.getBx() + "," + (int) d.getBy() + "," + (int) d.getBz() : "&7Not set - /wywa setbase"));
        inv.setItem(16, icon(Material.ENDER_EYE, "&5Return Tour", "&7Take a cinematic tour", "&7of your home"));
        inv.setItem(22, icon(Material.BOOK, "&6Your Journal", "&7Firsts, milestones and deaths", "&7Entries: &f" + d.getJournal().size()));

        player.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof HomeHolder)) return;
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player p)) return;

        switch (event.getRawSlot()) {
            case 10 -> { p.closeInventory(); plugin.getChronicle().reopen(p); }
            case 11 -> plugin.getDaily().claim(p);
            case 12 -> plugin.getDaily().streakInfo(p);
            case 13 -> { p.closeInventory(); plugin.getGuestbook().view(p); }
            case 14 -> { p.closeInventory(); plugin.getWatcher().summon(p); }
            case 15 -> {
                p.closeInventory();
                PlayerData bd = plugin.getStore().get(p.getUniqueId());
                if (bd.hasBase()) plugin.sendRaw(p, "base-info", "world", bd.getBaseWorld(),
                        "x", (int) bd.getBx(), "y", (int) bd.getBy(), "z", (int) bd.getBz());
                else plugin.send(p, "no-base");
            }
            case 16 -> { p.closeInventory(); plugin.getTour().start(p); }
            case 22 -> { p.closeInventory(); plugin.getJournal().view(p); }
            default -> { }
        }
    }

    private ItemStack icon(Material material, String name, String... lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
            meta.setLore(Arrays.stream(lore)
                    .map(l -> ChatColor.translateAlternateColorCodes('&', l)).toList());
            item.setItemMeta(meta);
        }
        return item;
    }
}
