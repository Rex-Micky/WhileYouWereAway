package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.DyeColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Mob;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.entity.Wolf;
import org.bukkit.persistence.PersistentDataType;

/**
 * The Watcher — a guardian that lives at your base, hunts intruders while
 * you're offline, and grows stronger with your loyalty.
 *
 * The mob type is configurable. It defaults to IRON_GOLEM because tamed
 * companions (wolves, cats) have owner-defence AI that ignores a manually
 * set target, so they will not actually chase an intruder.
 */
public class WatcherService {

    private final WhileYouWereAwayPlugin plugin;
    private final LoyaltyService loyalty;
    private final NamespacedKey keyOwner;

    public WatcherService(WhileYouWereAwayPlugin plugin, LoyaltyService loyalty) {
        this.plugin = plugin;
        this.loyalty = loyalty;
        this.keyOwner = new NamespacedKey(plugin, "watcher_owner");
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("watcher.enabled", true);
    }

    private EntityType mobType() {
        String raw = plugin.getConfig().getString("watcher.mob-type", "IRON_GOLEM");
        try {
            return EntityType.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            plugin.getLogger().warning("Unknown watcher.mob-type '" + raw + "', falling back to IRON_GOLEM");
            return EntityType.IRON_GOLEM;
        }
    }

    /** Summon (or move) the player's Watcher to their base. The player must be at the base. */
    public void summon(Player player) {
        if (!enabled()) { plugin.send(player, "watcher-disabled"); return; }
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        d.setName(player.getName());
        if (!d.hasBase()) { plugin.send(player, "no-base"); return; }

        World w = Bukkit.getWorld(d.getBaseWorld());
        if (w == null) { plugin.send(player, "no-base"); return; }
        int cx = (int) Math.floor(d.getBx()) >> 4, cz = (int) Math.floor(d.getBz()) >> 4;
        if (!w.isChunkLoaded(cx, cz)) { plugin.send(player, "watcher-far"); return; }

        remove(d);
        Location loc = new Location(w, d.getBx() + 0.5, d.getBy(), d.getBz() + 0.5);
        int level = loyalty.tierIndex(d.getCurrentStreak()) + 1;

        Entity spawned = w.spawnEntity(loc, mobType());
        if (!(spawned instanceof LivingEntity guard)) {
            spawned.remove();
            plugin.send(player, "watcher-disabled");
            return;
        }
        d.setWatcherEntity(guard.getUniqueId());
        d.setWatcherLevel(level);
        applyTraits(guard, d, level, player);
        plugin.getStore().save();
        plugin.send(player, "watcher-summoned", "level", level);
    }

    private void applyTraits(LivingEntity guard, PlayerData d, int level, Player owner) {
        guard.setCustomName(ChatColor.translateAlternateColorCodes('&',
                "&6" + d.getName() + "'s Watcher &7[Lv." + level + "]"));
        guard.setCustomNameVisible(true);
        guard.setPersistent(true);
        guard.setRemoveWhenFarAway(false);
        guard.setGlowing(true);
        guard.setInvulnerable(plugin.getConfig().getBoolean("watcher.invulnerable", true));

        // Only tame mobs that can be tamed — and note this makes them owner-defensive.
        if (guard instanceof Tameable tameable && owner != null) {
            tameable.setTamed(true);
            tameable.setOwner(owner);
        }
        if (guard instanceof Wolf wolf) wolf.setCollarColor(collarFor(level));

        AttributeInstance hp = Compat.MAX_HEALTH == null ? null : guard.getAttribute(Compat.MAX_HEALTH);
        if (hp != null) {
            hp.setBaseValue(20.0 + level * 10.0);
            guard.setHealth(hp.getValue());
        }
        AttributeInstance dmg = Compat.ATTACK_DAMAGE == null ? null : guard.getAttribute(Compat.ATTACK_DAMAGE);
        if (dmg != null) dmg.setBaseValue(4.0 + level * 2.0);

        guard.getPersistentDataContainer().set(keyOwner, PersistentDataType.STRING, d.getUuid().toString());
    }

    private DyeColor collarFor(int level) {
        return switch (Math.min(level, 5)) {
            case 1 -> DyeColor.WHITE;
            case 2 -> DyeColor.LIME;
            case 3 -> DyeColor.LIGHT_BLUE;
            case 4 -> DyeColor.MAGENTA;
            default -> DyeColor.ORANGE;
        };
    }

    /** Re-level an existing Watcher to match the owner's current loyalty. */
    public void refresh(PlayerData d) {
        if (d.getWatcherEntity() == null) return;
        if (!(Bukkit.getEntity(d.getWatcherEntity()) instanceof LivingEntity guard) || !guard.isValid()) return;
        int level = loyalty.tierIndex(d.getCurrentStreak()) + 1;
        d.setWatcherLevel(level);
        applyTraits(guard, d, level, Bukkit.getPlayer(d.getUuid()));
    }

    /** Watchers whose owner is offline hunt the nearest intruder. */
    public void guardTick() {
        if (!enabled()) return;
        double range = plugin.getConfig().getDouble("watcher.guard-range", 16);
        for (PlayerData d : plugin.getStore().all()) {
            if (d.getWatcherEntity() == null) continue;
            if (!(Bukkit.getEntity(d.getWatcherEntity()) instanceof LivingEntity guard) || !guard.isValid()) continue;
            if (!(guard instanceof Mob mob)) continue;

            if (Bukkit.getPlayer(d.getUuid()) != null) {   // owner online — stand down
                mob.setTarget(null);
                if (guard instanceof Wolf wolf) wolf.setAngry(false);
                continue;
            }
            Player intruder = null;
            double best = range * range;
            for (Player pl : guard.getWorld().getPlayers()) {
                if (pl.getUniqueId().equals(d.getUuid())) continue;
                if (pl.getGameMode() == GameMode.SPECTATOR || pl.getGameMode() == GameMode.CREATIVE) continue;
                double dist = pl.getLocation().distanceSquared(guard.getLocation());
                if (dist < best) { best = dist; intruder = pl; }
            }
            if (intruder != null) {
                if (guard instanceof Wolf wolf) wolf.setAngry(true);
                mob.setTarget(intruder);
            }
        }
    }

    public void remove(PlayerData d) {
        if (d.getWatcherEntity() == null) return;
        Entity e = Bukkit.getEntity(d.getWatcherEntity());
        if (e != null) e.remove();
        d.setWatcherEntity(null);
    }
}
