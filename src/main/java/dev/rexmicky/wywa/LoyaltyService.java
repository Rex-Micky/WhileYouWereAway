package dev.rexmicky.wywa;

import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.entity.Display;
import org.bukkit.entity.Entity;
import org.bukkit.entity.TextDisplay;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.Map;

/**
 * Turns the login streak into visible "loyalty": a tier, and a floating shrine
 * totem that grows/upgrades above the player's base as they keep returning.
 */
public class LoyaltyService {

    private final WhileYouWereAwayPlugin plugin;
    private final NamespacedKey keyShrineOwner;

    public LoyaltyService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
        this.keyShrineOwner = new NamespacedKey(plugin, "shrine_owner");
    }

    /** Highest tier index whose threshold the streak has reached (0-based). */
    public int tierIndex(int streak) {
        List<Map<?, ?>> tiers = plugin.getConfig().getMapList("loyalty.tiers");
        int best = 0;
        for (int i = 0; i < tiers.size(); i++) {
            Object at = tiers.get(i).get("at");
            int threshold = at instanceof Number n ? n.intValue() : 0;
            if (streak >= threshold) best = i;
        }
        return best;
    }

    public String tierName(int streak) {
        List<Map<?, ?>> tiers = plugin.getConfig().getMapList("loyalty.tiers");
        if (tiers.isEmpty()) return "&7Loyal";
        Object name = tiers.get(Math.min(tierIndex(streak), tiers.size() - 1)).get("name");
        return name == null ? "&7Loyal" : String.valueOf(name);
    }

    /** Spawn or refresh the loyalty shrine above the player's base. Safe if the base is unloaded. */
    public void updateShrine(PlayerData d) {
        if (!plugin.getConfig().getBoolean("loyalty.enabled", true)) return;
        if (!plugin.getConfig().getBoolean("loyalty.shrine", true)) return;
        if (!d.hasBase()) return;

        World w = Bukkit.getWorld(d.getBaseWorld());
        if (w == null) return;
        int cx = (int) Math.floor(d.getBx()) >> 4, cz = (int) Math.floor(d.getBz()) >> 4;
        if (!w.isChunkLoaded(cx, cz)) return;

        Location loc = new Location(w, d.getBx() + 0.5, d.getBy() + 2.6, d.getBz() + 0.5);
        TextDisplay td = null;
        if (d.getShrineEntity() != null && Bukkit.getEntity(d.getShrineEntity()) instanceof TextDisplay existing
                && existing.isValid()) {
            td = existing;
            td.teleport(loc);
        }
        if (td == null) {
            // Sweep up any stray shrines we lost track of, so they don't pile up at the base.
            for (Entity near : w.getNearbyEntities(loc, 4, 4, 4)) {
                if (near instanceof TextDisplay stray && d.getUuid().toString().equals(
                        stray.getPersistentDataContainer().get(keyShrineOwner, PersistentDataType.STRING))) {
                    stray.remove();
                }
            }
            td = w.spawn(loc, TextDisplay.class, t -> t.setBillboard(Display.Billboard.CENTER));
            td.getPersistentDataContainer().set(keyShrineOwner, PersistentDataType.STRING, d.getUuid().toString());
            d.setShrineEntity(td.getUniqueId());
        }

        int streak = d.getCurrentStreak();
        String raw = "&8❈ &f" + d.getName() + "'s Shrine &8❈\n" + tierName(streak) + "\n&7" + streak + "-day loyalty";
        td.text(LegacyComponentSerializer.legacyAmpersand().deserialize(raw));
    }

    public void removeShrine(PlayerData d) {
        if (d.getShrineEntity() == null) return;
        Entity e = Bukkit.getEntity(d.getShrineEntity());
        if (e != null) e.remove();
        d.setShrineEntity(null);
    }
}
