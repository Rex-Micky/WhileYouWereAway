package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
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
            PlayerData self = plugin.getStore().get(p.getUniqueId());
            long away = self.getLastLogout() == 0 ? 0 : System.currentTimeMillis() - self.getLastLogout();

            plugin.getRecap().deliver(p);
            plugin.getIdleRewards().grantIfEligible(p, away);
            plugin.getDaily().notifyIfAvailable(p);
            plugin.getGuestbook().deliverGifts(p);
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

    // --- journal: the player's story, written as it happens ---------------------

    @EventHandler(priority = EventPriority.MONITOR)
    public void onAdvancement(PlayerAdvancementDoneEvent event) {
        String key = event.getAdvancement().getKey().getKey();
        if (key.startsWith("recipes/")) return;              // recipe unlocks are noise
        String name = key.contains("/") ? key.substring(key.lastIndexOf('/') + 1) : key;
        plugin.getJournal().record(event.getPlayer(), "advancement", prettify(name));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerDeath(PlayerDeathEvent event) {
        String message = event.getDeathMessage();
        plugin.getJournal().record(event.getEntity(), "death",
                message == null || message.isBlank() ? "Died" : message);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        Material type = event.getItem().getItemStack().getType();
        String milestone = switch (type) {
            case DIAMOND -> "Found their first diamond";
            case ANCIENT_DEBRIS, NETHERITE_SCRAP -> "Unearthed their first ancient debris";
            case NETHER_STAR -> "Claimed their first nether star";
            default -> null;
        };
        if (milestone != null) plugin.getJournal().recordOnce(player, "first", milestone);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onBossKill(EntityDeathEvent event) {
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;
        EntityType type = event.getEntity().getType();
        if (type == EntityType.ENDER_DRAGON) {
            plugin.getJournal().record(killer, "boss", "Slew the Ender Dragon");
        } else if (type == EntityType.WITHER) {
            plugin.getJournal().record(killer, "boss", "Slew the Wither");
        } else if (type == EntityType.WARDEN) {
            plugin.getJournal().record(killer, "boss", "Slew a Warden");
        }
    }

    private String prettify(String raw) {
        String spaced = raw.replace('_', ' ');
        return spaced.isEmpty() ? spaced : Character.toUpperCase(spaced.charAt(0)) + spaced.substring(1);
    }
}
