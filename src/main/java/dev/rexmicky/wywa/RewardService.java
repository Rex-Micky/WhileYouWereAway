package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.Map;

/**
 * Grants a reward defined by a config section - items, XP and/or console commands.
 * Reused by daily rewards, milestones, the comeback bonus and away-rewards.
 */
public class RewardService {

    private final WhileYouWereAwayPlugin plugin;

    public RewardService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    public void give(Player player, ConfigurationSection reward) {
        if (reward == null) return;

        int xp = reward.getInt("xp", 0);
        if (xp > 0) player.giveExp(xp);

        for (String spec : reward.getStringList("items")) {
            ItemStack item = parseItem(spec);
            if (item == null) continue;
            Map<Integer, ItemStack> overflow = player.getInventory().addItem(item);
            for (ItemStack leftover : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), leftover);
            }
        }

        for (String cmd : reward.getStringList("commands")) {
            if (cmd == null || cmd.isBlank()) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
        }
    }

    /**
     * Give a reward multiplied by a factor - used by idle rewards (per-hour x hours).
     * Items are handed out one batch at a time so stacks split naturally instead of
     * building an oversized ItemStack. Commands run once, not once per multiple.
     */
    public void giveScaled(Player player, ConfigurationSection reward, int multiplier) {
        if (reward == null || multiplier <= 0) return;

        int xp = reward.getInt("xp", 0) * multiplier;
        if (xp > 0) player.giveExp(xp);

        for (int i = 0; i < multiplier; i++) {
            for (String spec : reward.getStringList("items")) {
                ItemStack item = parseItem(spec);
                if (item == null) continue;
                for (ItemStack leftover : player.getInventory().addItem(item).values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), leftover);
                }
            }
        }
        for (String cmd : reward.getStringList("commands")) {
            if (cmd == null || cmd.isBlank()) continue;
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.replace("{player}", player.getName()));
        }
    }

    private ItemStack parseItem(String spec) {
        String[] parts = spec.split(":");
        Material mat = Material.matchMaterial(parts[0].trim());
        if (mat == null) {
            plugin.getLogger().warning("Unknown reward item in config: " + spec);
            return null;
        }
        int amount = 1;
        if (parts.length > 1) {
            try { amount = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
        }
        return new ItemStack(mat, Math.max(1, amount));
    }
}
