package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * A short cinematic "welcome home" - flies the camera (spectator) around the
 * player's base, then restores their exact gamemode and position. Restore is
 * guaranteed on completion, disconnect or death.
 */
public class TourService {

    private record Restore(GameMode mode, Location loc) {}

    private static final String[] CARDS = {
            "§6§lWelcome home", "§eYour base stood watch", "§eWhile you were away...", "§aAll is as you left it"
    };
    private static final int[][] OFFSETS = { {10, 8, 10}, {-10, 8, 10}, {-10, 8, -10}, {10, 8, -10} };

    private final WhileYouWereAwayPlugin plugin;
    private final Map<UUID, Restore> active = new HashMap<>();

    public TourService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("return-tour.enabled", true);
    }

    public boolean isTouring(Player p) {
        return active.containsKey(p.getUniqueId());
    }

    public void start(Player player) {
        if (!enabled()) { plugin.send(player, "tour-disabled"); return; }
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        if (!d.hasBase()) { plugin.send(player, "no-base"); return; }
        if (isTouring(player)) return;
        World w = Bukkit.getWorld(d.getBaseWorld());
        if (w == null) { plugin.send(player, "no-base"); return; }

        Location center = new Location(w, d.getBx() + 0.5, d.getBy() + 1, d.getBz() + 0.5);
        List<Location> points = new ArrayList<>();
        for (int[] o : OFFSETS) {
            Location cam = center.clone().add(o[0], o[1], o[2]);
            cam.setDirection(center.toVector().subtract(cam.toVector()));
            points.add(cam);
        }

        active.put(player.getUniqueId(), new Restore(player.getGameMode(), player.getLocation().clone()));
        player.setGameMode(GameMode.SPECTATOR);
        runStep(player, points, 0);
    }

    private void runStep(Player player, List<Location> points, int i) {
        if (!player.isOnline() || !isTouring(player)) { abort(player); return; }
        if (i >= points.size()) { finish(player); return; }
        player.teleport(points.get(i));
        player.sendTitle(CARDS[i % CARDS.length], "", 8, 34, 8);
        Bukkit.getScheduler().runTaskLater(plugin, () -> runStep(player, points, i + 1), 50L);
    }

    private void finish(Player player) {
        abort(player);
        if (player.isOnline()) plugin.send(player, "tour-done");
    }

    /** Restore the player immediately - used on completion, quit or death. */
    public void abort(Player player) {
        Restore r = active.remove(player.getUniqueId());
        if (r == null) return;
        if (player.isOnline()) {
            player.setGameMode(r.mode());
            player.teleport(r.loc());
        }
    }
}
