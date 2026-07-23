package dev.rexmicky.wywa;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

import java.time.LocalDate;

/** Daily login rewards with a running streak and milestone bonuses. */
public class DailyService {

    private final WhileYouWereAwayPlugin plugin;
    private final RewardService rewards;

    public DailyService(WhileYouWereAwayPlugin plugin, RewardService rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("daily-rewards.enabled", true);
    }

    public boolean isAvailable(PlayerData d) {
        return enabled() && !LocalDate.now().toString().equals(d.getLastClaimDate());
    }

    /** Called after login - nudges the player if today's reward is waiting. */
    public void notifyIfAvailable(Player player) {
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        if (isAvailable(d)) {
            plugin.send(player, "daily-available", "streak", Math.max(1, d.getCurrentStreak()));
        }
    }

    public void claim(Player player) {
        if (!enabled()) { plugin.send(player, "daily-disabled"); return; }

        PlayerData d = plugin.getStore().get(player.getUniqueId());
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        if (todayStr.equals(d.getLastClaimDate())) { plugin.send(player, "daily-already"); return; }

        d.setCurrentStreak(nextStreak(d, today));
        d.setLongestStreak(Math.max(d.getLongestStreak(), d.getCurrentStreak()));
        d.setLastClaimDate(todayStr);
        d.setTotalClaims(d.getTotalClaims() + 1);

        rewards.give(player, plugin.getConfig().getConfigurationSection("daily-rewards.reward"));

        ConfigurationSection milestone =
                plugin.getConfig().getConfigurationSection("daily-rewards.milestones." + d.getCurrentStreak());
        if (milestone != null) {
            rewards.give(player, milestone);
            String msg = milestone.getString("message");
            if (msg != null && !msg.isEmpty()) player.sendMessage(plugin.color(msg));
        }

        plugin.getStore().save();
        plugin.send(player, "daily-claimed", "streak", d.getCurrentStreak(), "longest", d.getLongestStreak());
    }

    public void streakInfo(Player player) {
        PlayerData d = plugin.getStore().get(player.getUniqueId());
        plugin.send(player, "streak-info",
                "streak", d.getCurrentStreak(), "longest", d.getLongestStreak(), "total", d.getTotalClaims());
    }

    private int nextStreak(PlayerData d, LocalDate today) {
        if (d.getLastClaimDate().isEmpty()) return 1;
        LocalDate last;
        try { last = LocalDate.parse(d.getLastClaimDate()); } catch (Exception ex) { return 1; }
        if (last.equals(today.minusDays(1))) return d.getCurrentStreak() + 1;          // consecutive day
        if (!plugin.getConfig().getBoolean("daily-rewards.reset-on-miss", true)) {
            return d.getCurrentStreak() + 1;                                            // forgiving mode
        }
        return 1;                                                                        // missed a day
    }
}
