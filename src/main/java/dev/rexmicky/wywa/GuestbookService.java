package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;

/** Guestbook signing at offline players' bases, plus leaving item gifts. */
public class GuestbookService {

    private final WhileYouWereAwayPlugin plugin;

    public GuestbookService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    /** Sign the guestbook of whichever offline player's base you're standing near. */
    public void sign(Player player, String message) {
        double radius = plugin.getConfig().getDouble("base.radius", 24);
        double best = radius * radius;
        PlayerData target = null;
        for (PlayerData d : plugin.getStore().all()) {
            if (!d.hasBase() || d.getUuid().equals(player.getUniqueId())) continue;
            if (Bukkit.getPlayer(d.getUuid()) != null) continue;                 // owner is online
            if (!player.getWorld().getName().equals(d.getBaseWorld())) continue;
            double dist = player.getLocation().distanceSquared(new Location(player.getWorld(), d.getBx(), d.getBy(), d.getBz()));
            if (dist < best) { best = dist; target = d; }
        }
        if (target == null) { plugin.send(player, "guestbook-not-near"); return; }

        target.getGuestbook().add(new PlayerData.Mail(player.getName(), message, System.currentTimeMillis()));
        plugin.getStore().save();
        plugin.send(player, "guestbook-signed", "player", target.getName());
    }

    /** Show the player their own guestbook. */
    public void view(Player player) {
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        List<PlayerData.Mail> book = d.getGuestbook();
        if (book.isEmpty()) { plugin.send(player, "guestbook-empty"); return; }
        plugin.send(player, "guestbook-header", "count", book.size());
        int start = Math.max(0, book.size() - 10);
        for (int i = start; i < book.size(); i++) {
            PlayerData.Mail e = book.get(i);
            plugin.sendRaw(player, "guestbook-entry", "from", e.from, "message", e.message, "ago", ago(e.time));
        }
    }

    /** Leave the item in your hand as a gift for another player. */
    public void gift(Player player, String targetName) {
        ItemStack held = player.getInventory().getItemInMainHand();
        if (held == null || held.getType().isAir()) { plugin.send(player, "gift-none"); return; }
        if (player.getName().equalsIgnoreCase(targetName)) { plugin.send(player, "gift-self"); return; }

        ItemStack gift = held.clone();
        player.getInventory().setItemInMainHand(null);

        Player online = Bukkit.getPlayerExact(targetName);
        if (online != null) {
            for (ItemStack leftover : online.getInventory().addItem(gift).values())
                online.getWorld().dropItemNaturally(online.getLocation(), leftover);
            plugin.send(online, "gift-received", "from", player.getName());
            plugin.send(player, "gift-sent", "player", online.getName());
            return;
        }
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        PlayerData d = plugin.getStore().get(off.getUniqueId());
        d.setName(targetName);
        d.getGifts().add(gift);
        plugin.getStore().save();
        plugin.send(player, "gift-sent", "player", targetName);
    }

    /** Deliver any queued gifts to a player on login. */
    public void deliverGifts(Player player) {
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        if (d.getGifts().isEmpty()) return;
        int count = d.getGifts().size();
        for (ItemStack gift : d.getGifts()) {
            for (ItemStack leftover : player.getInventory().addItem(gift).values())
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
        }
        d.getGifts().clear();
        plugin.getStore().save();
        plugin.send(player, "gifts-delivered", "count", count);
    }

    private String ago(long time) {
        long secs = Math.max(0, (System.currentTimeMillis() - time) / 1000);
        if (secs < 60) return secs + "s ago";
        if (secs < 3600) return (secs / 60) + "m ago";
        if (secs < 86400) return (secs / 3600) + "h ago";
        return (secs / 86400) + "d ago";
    }
}
