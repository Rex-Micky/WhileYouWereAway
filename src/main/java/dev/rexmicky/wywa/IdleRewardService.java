package dev.rexmicky.wywa;

import org.bukkit.entity.Player;

/**
 * Rewards that quietly accrue while you're offline, claimed the moment you return.
 * Capped, so someone who vanishes for a month doesn't come back to a fortune.
 */
public class IdleRewardService {

    private final WhileYouWereAwayPlugin plugin;
    private final RewardService rewards;

    public IdleRewardService(WhileYouWereAwayPlugin plugin, RewardService rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
    }

    /** @return true if anything was granted. */
    public boolean grantIfEligible(Player player, long awayMillis) {
        if (!plugin.getConfig().getBoolean("idle-rewards.enabled", true)) return false;

        long hours = awayMillis / 3_600_000L;
        long cap = plugin.getConfig().getLong("idle-rewards.max-hours", 8);
        long counted = Math.min(hours, Math.max(0, cap));
        if (counted < 1) return false;

        rewards.giveScaled(player, plugin.getConfig().getConfigurationSection("idle-rewards.per-hour"),
                (int) counted);
        plugin.send(player, "idle-granted", "hours", counted);
        return true;
    }
}
