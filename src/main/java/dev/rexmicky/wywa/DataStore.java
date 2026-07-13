package dev.rexmicky.wywa;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

/** Loads, holds and persists every player's {@link PlayerData}. */
public class DataStore {

    private final WhileYouWereAwayPlugin plugin;
    private final File file;
    private final Map<UUID, PlayerData> players = new LinkedHashMap<>();

    public DataStore(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "playerdata.yml");
    }

    public PlayerData get(UUID uuid) {
        return players.computeIfAbsent(uuid, PlayerData::new);
    }

    public Collection<PlayerData> all() {
        return new ArrayList<>(players.values());
    }

    public void load() {
        players.clear();
        if (!file.exists()) return;
        YamlConfiguration yml = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection root = yml.getConfigurationSection("players");
        if (root == null) return;

        for (String key : root.getKeys(false)) {
            ConfigurationSection s = root.getConfigurationSection(key);
            if (s == null) continue;
            try {
                PlayerData d = new PlayerData(UUID.fromString(key));
                d.setName(s.getString("name", "Unknown"));
                d.setLastLogout(s.getLong("lastLogout"));
                d.setWorldTimeAtQuit(s.getLong("worldTimeAtQuit"));
                if (s.getBoolean("hasBase", false)) {
                    d.setBaseRaw(s.getString("base.world"),
                            s.getDouble("base.x"), s.getDouble("base.y"), s.getDouble("base.z"));
                }
                d.getJoinedWhileAway().addAll(s.getStringList("pending.joined"));
                d.getNewPlayersWhileAway().addAll(s.getStringList("pending.newPlayers"));
                ConfigurationSection visits = s.getConfigurationSection("pending.visits");
                if (visits != null) {
                    for (String v : visits.getKeys(false)) d.getBaseVisits().put(v, visits.getInt(v));
                }
                for (Map<?, ?> m : s.getMapList("pending.mail")) {
                    d.getMail().add(new PlayerData.Mail(
                            String.valueOf(m.get("from")),
                            String.valueOf(m.get("message")),
                            m.get("time") instanceof Number n ? n.longValue() : 0L));
                }
                d.setLastClaimDate(s.getString("daily.lastClaimDate", ""));
                d.setCurrentStreak(s.getInt("daily.currentStreak", 0));
                d.setLongestStreak(s.getInt("daily.longestStreak", 0));
                d.setTotalClaims(s.getInt("daily.totalClaims", 0));
                players.put(d.getUuid(), d);
            } catch (Exception ex) {
                plugin.getLogger().log(Level.WARNING, "Skipping malformed player data '" + key + "'", ex);
            }
        }
        plugin.getLogger().info("Loaded data for " + players.size() + " player(s).");
    }

    public void save() {
        YamlConfiguration yml = new YamlConfiguration();
        for (PlayerData d : players.values()) {
            String p = "players." + d.getUuid();
            yml.set(p + ".name", d.getName());
            yml.set(p + ".lastLogout", d.getLastLogout());
            yml.set(p + ".worldTimeAtQuit", d.getWorldTimeAtQuit());
            yml.set(p + ".hasBase", d.hasBase());
            if (d.hasBase()) {
                yml.set(p + ".base.world", d.getBaseWorld());
                yml.set(p + ".base.x", d.getBx());
                yml.set(p + ".base.y", d.getBy());
                yml.set(p + ".base.z", d.getBz());
            }
            if (!d.getJoinedWhileAway().isEmpty())
                yml.set(p + ".pending.joined", new ArrayList<>(d.getJoinedWhileAway()));
            if (!d.getNewPlayersWhileAway().isEmpty())
                yml.set(p + ".pending.newPlayers", new ArrayList<>(d.getNewPlayersWhileAway()));
            if (!d.getBaseVisits().isEmpty()) {
                for (Map.Entry<String, Integer> e : d.getBaseVisits().entrySet())
                    yml.set(p + ".pending.visits." + e.getKey(), e.getValue());
            }
            if (!d.getMail().isEmpty()) {
                List<Map<String, Object>> mails = new ArrayList<>();
                for (PlayerData.Mail m : d.getMail()) {
                    Map<String, Object> mm = new LinkedHashMap<>();
                    mm.put("from", m.from);
                    mm.put("message", m.message);
                    mm.put("time", m.time);
                    mails.add(mm);
                }
                yml.set(p + ".pending.mail", mails);
            }
            if (!d.getLastClaimDate().isEmpty()) {
                yml.set(p + ".daily.lastClaimDate", d.getLastClaimDate());
                yml.set(p + ".daily.currentStreak", d.getCurrentStreak());
                yml.set(p + ".daily.longestStreak", d.getLongestStreak());
                yml.set(p + ".daily.totalClaims", d.getTotalClaims());
            }
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save playerdata.yml", ex);
        }
    }
}
