package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class WywaListener implements Listener {

    private final WhileYouWereAwayPlugin plugin;

    public WywaListener(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        boolean newPlayer = !p.hasPlayedBefore();

        // Note this join in every currently-offline player's pending recap.
        for (PlayerData o : plugin.getStore().all()) {
            if (o.getUuid().equals(p.getUniqueId())) continue;
            if (Bukkit.getPlayer(o.getUuid()) != null) continue; // online players see it live
            o.getJoinedWhileAway().add(p.getName());
            if (newPlayer) o.getNewPlayersWhileAway().add(p.getName());
        }
        plugin.getStore().get(p.getUniqueId()).setName(p.getName());

        // Deliver this player's own recap once they've finished loading in.
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            if (!p.isOnline()) return;
            plugin.getRecap().deliver(p);
            plugin.getDaily().notifyIfAvailable(p);
            plugin.getGuestbook().deliverGifts(p);
            PlayerData self = plugin.getStore().get(p.getUniqueId());
            plugin.getLoyalty().updateShrine(self);
            plugin.getWatcher().refresh(self);
        }, 40L);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();
        plugin.getTour().abort(p);   // never leave a disconnecting player stuck in spectator
        PlayerData d = plugin.getStore().get(p.getUniqueId());
        d.setName(p.getName());
        d.setLastLogout(System.currentTimeMillis());
        if (!Bukkit.getWorlds().isEmpty()) d.setWorldTimeAtQuit(Bukkit.getWorlds().get(0).getFullTime());
        if (!d.hasBase()) {
            Location bed = p.getBedSpawnLocation();
            d.setBase(bed != null ? bed : p.getLocation());
        }
        plugin.getStore().save();
    }
}
