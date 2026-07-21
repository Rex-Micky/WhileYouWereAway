package dev.rexmicky.wywa;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

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
                            m.get("time") instanceof Number n ? n.longValue() : 0L,
                            m.get("deliverAt") instanceof Number dn ? dn.longValue() : 0L));
                }
                for (Map<?, ?> g : s.getMapList("guestbook")) {
                    d.getGuestbook().add(new PlayerData.Mail(
                            String.valueOf(g.get("from")),
                            String.valueOf(g.get("message")),
                            g.get("time") instanceof Number gn ? gn.longValue() : 0L));
                }
                String shrine = s.getString("shrineEntity");
                if (shrine != null) try { d.setShrineEntity(UUID.fromString(shrine)); } catch (Exception ignored) {}
                String watcher = s.getString("watcherEntity");
                if (watcher != null) try { d.setWatcherEntity(UUID.fromString(watcher)); } catch (Exception ignored) {}
                d.setWatcherLevel(s.getInt("watcherLevel", 0));
                List<?> giftList = s.getList("gifts");
                if (giftList != null) for (Object o : giftList) if (o instanceof ItemStack is) d.getGifts().add(is);
                for (Map<?, ?> j : s.getMapList("journal")) {
                    d.getJournal().add(new PlayerData.Mail(
                            String.valueOf(j.get("from")),
                            String.valueOf(j.get("message")),
                            j.get("time") instanceof Number jn ? jn.longValue() : 0L));
                }
                d.setDiscordWebhook(s.getString("discordWebhook"));
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
                    mm.put("deliverAt", m.deliverAt);
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
            if (!d.getGuestbook().isEmpty()) {
                List<Map<String, Object>> book = new ArrayList<>();
                for (PlayerData.Mail g : d.getGuestbook()) {
                    Map<String, Object> gm = new LinkedHashMap<>();
                    gm.put("from", g.from);
                    gm.put("message", g.message);
                    gm.put("time", g.time);
                    book.add(gm);
                }
                yml.set(p + ".guestbook", book);
            }
            if (!d.getGifts().isEmpty()) yml.set(p + ".gifts", d.getGifts());
            if (!d.getJournal().isEmpty()) {
                List<Map<String, Object>> entries = new ArrayList<>();
                for (PlayerData.Mail j : d.getJournal()) {
                    Map<String, Object> jm = new LinkedHashMap<>();
                    jm.put("from", j.from);
                    jm.put("message", j.message);
                    jm.put("time", j.time);
                    entries.add(jm);
                }
                yml.set(p + ".journal", entries);
            }
            if (d.getDiscordWebhook() != null) yml.set(p + ".discordWebhook", d.getDiscordWebhook());
            if (d.getShrineEntity() != null) yml.set(p + ".shrineEntity", d.getShrineEntity().toString());
            if (d.getWatcherEntity() != null) yml.set(p + ".watcherEntity", d.getWatcherEntity().toString());
            if (d.getWatcherLevel() > 0) yml.set(p + ".watcherLevel", d.getWatcherLevel());
        }
        try {
            yml.save(file);
        } catch (IOException ex) {
            plugin.getLogger().log(Level.SEVERE, "Could not save playerdata.yml", ex);
        }
    }
}
