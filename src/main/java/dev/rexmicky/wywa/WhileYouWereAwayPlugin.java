package dev.rexmicky.wywa;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class WhileYouWereAwayPlugin extends JavaPlugin {

    private DataStore store;
    private RecapService recap;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        store = new DataStore(this);
        store.load();
        recap = new RecapService(this);

        getServer().getPluginManager().registerEvents(new WywaListener(this), this);
        WywaCommand cmd = new WywaCommand(this);
        getCommand("wywa").setExecutor(cmd);
        getCommand("wywa").setTabCompleter(cmd);

        // Watch for players wandering near offline players' bases (every 10s).
        getServer().getScheduler().runTaskTimer(this, this::proximityTick, 200L, 200L);
        // Periodic autosave so recorded events survive a crash (every 60s).
        getServer().getScheduler().runTaskTimer(this, () -> store.save(), 1200L, 1200L);

        getLogger().info("WhileYouWereAway enabled.");
    }

    @Override
    public void onDisable() {
        if (store == null) return;
        // Treat a shutdown as everyone logging off, so time-away is accurate after a restart.
        long now = System.currentTimeMillis();
        long worldTime = getServer().getWorlds().isEmpty() ? 0 : getServer().getWorlds().get(0).getFullTime();
        for (Player p : getServer().getOnlinePlayers()) {
            PlayerData d = store.get(p.getUniqueId());
            d.setName(p.getName());
            d.setLastLogout(now);
            d.setWorldTimeAtQuit(worldTime);
            if (!d.hasBase()) {
                Location bed = p.getBedSpawnLocation();
                d.setBase(bed != null ? bed : p.getLocation());
            }
        }
        store.save();
    }

    public DataStore getStore() { return store; }
    public RecapService getRecap() { return recap; }

    private void proximityTick() {
        double radius = getConfig().getDouble("base.radius", 24);
        double r2 = radius * radius;
        long cooldown = getConfig().getLong("base.visit-cooldown-seconds", 120) * 1000L;
        long now = System.currentTimeMillis();

        for (Player v : getServer().getOnlinePlayers()) {
            String vWorld = v.getWorld().getName();
            double vx = v.getLocation().getX(), vy = v.getLocation().getY(), vz = v.getLocation().getZ();
            for (PlayerData d : store.all()) {
                if (!d.hasBase() || d.getUuid().equals(v.getUniqueId())) continue;
                if (getServer().getPlayer(d.getUuid()) != null) continue;      // base owner is online
                if (!vWorld.equals(d.getBaseWorld())) continue;
                double dx = vx - d.getBx(), dy = vy - d.getBy(), dz = vz - d.getBz();
                if (dx * dx + dy * dy + dz * dz > r2) continue;
                long last = d.getVisitCooldowns().getOrDefault(v.getName(), 0L);
                if (now - last >= cooldown) {
                    d.getBaseVisits().merge(v.getName(), 1, Integer::sum);
                    d.getVisitCooldowns().put(v.getName(), now);
                }
            }
        }
    }

    // --- messaging -----------------------------------------------------------

    public String format(String key, Object... placeholders) {
        String raw = getConfig().getString("messages." + key, "");
        for (int i = 0; i + 1 < placeholders.length; i += 2) {
            raw = raw.replace("{" + placeholders[i] + "}", String.valueOf(placeholders[i + 1]));
        }
        return ChatColor.translateAlternateColorCodes('&', raw);
    }

    public void send(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(format("prefix") + format(key, placeholders));
    }

    public void sendRaw(CommandSender to, String key, Object... placeholders) {
        to.sendMessage(format(key, placeholders));
    }
}
