package dev.rexmicky.wywa;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

/** A welcome-back reward that scales with how long the player was away. */
public class ComebackService {

    private final WhileYouWereAwayPlugin plugin;
    private final RewardService rewards;

    public ComebackService(WhileYouWereAwayPlugin plugin, RewardService rewards) {
        this.plugin = plugin;
        this.rewards = rewards;
    }

    /**
     * Grants the highest comeback tier the player qualifies for after {@code awayMillis}.
     * @return true if a bonus was granted.
     */
    public boolean grantIfEligible(Player player, long awayMillis) {
        if (!plugin.getConfig().getBoolean("comeback.enabled", true)) return false;
        ConfigurationSection tiers = plugin.getConfig().getConfigurationSection("comeback.tiers");
        if (tiers == null) return false;

        long daysAway = awayMillis / 86_400_000L;
        int best = -1;
        for (String key : tiers.getKeys(false)) {
            try {
                int days = Integer.parseInt(key);
                if (days <= daysAway && days > best) best = days;
            } catch (NumberFormatException ignored) {}
        }
        if (best < 0) return false;

        ConfigurationSection tier = tiers.getConfigurationSection(String.valueOf(best));
        if (tier == null) return false;
        rewards.give(player, tier);
        String msg = tier.getString("message");
        if (msg != null && !msg.isEmpty()) player.sendMessage(plugin.color(msg));
        return true;
    }
}
