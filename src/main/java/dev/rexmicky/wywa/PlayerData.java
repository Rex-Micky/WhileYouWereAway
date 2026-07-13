package dev.rexmicky.wywa;

import org.bukkit.Location;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Everything we remember about one player, including what to tell them next time they log in. */
public class PlayerData {

    /** A piece of offline mail waiting to be delivered. */
    public static final class Mail {
        public final String from;
        public final String message;
        public final long time;
        public Mail(String from, String message, long time) {
            this.from = from;
            this.message = message;
            this.time = time;
        }
    }

    private final UUID uuid;
    private String name = "Unknown";
    private long lastLogout;        // epoch millis; 0 = we've never seen them leave
    private long worldTimeAtQuit;   // primary world full time when they logged off

    private boolean hasBase;
    private String baseWorld;
    private double bx, by, bz;

    // Accumulated while the player is offline; shown and cleared on their next login.
    private final Set<String> joinedWhileAway = new LinkedHashSet<>();
    private final Set<String> newPlayersWhileAway = new LinkedHashSet<>();
    private final Map<String, Integer> baseVisits = new LinkedHashMap<>();
    private final List<Mail> mail = new ArrayList<>();

    // Not persisted: throttles repeated base-visit counts per visitor.
    private final Map<String, Long> visitCooldowns = new HashMap<>();

    public PlayerData(UUID uuid) { this.uuid = uuid; }

    public UUID getUuid() { return uuid; }
    public String getName() { return name; }
    public void setName(String name) { if (name != null) this.name = name; }

    public long getLastLogout() { return lastLogout; }
    public void setLastLogout(long lastLogout) { this.lastLogout = lastLogout; }

    public long getWorldTimeAtQuit() { return worldTimeAtQuit; }
    public void setWorldTimeAtQuit(long t) { this.worldTimeAtQuit = t; }

    public boolean hasBase() { return hasBase; }
    public String getBaseWorld() { return baseWorld; }
    public double getBx() { return bx; }
    public double getBy() { return by; }
    public double getBz() { return bz; }

    public void setBase(Location loc) {
        if (loc == null || loc.getWorld() == null) return;
        this.hasBase = true;
        this.baseWorld = loc.getWorld().getName();
        this.bx = loc.getX();
        this.by = loc.getY();
        this.bz = loc.getZ();
    }

    public void setBaseRaw(String world, double x, double y, double z) {
        this.hasBase = world != null;
        this.baseWorld = world;
        this.bx = x; this.by = y; this.bz = z;
    }

    public Set<String> getJoinedWhileAway() { return joinedWhileAway; }
    public Set<String> getNewPlayersWhileAway() { return newPlayersWhileAway; }
    public Map<String, Integer> getBaseVisits() { return baseVisits; }
    public List<Mail> getMail() { return mail; }
    public Map<String, Long> getVisitCooldowns() { return visitCooldowns; }

    public boolean hasAnythingToReport() {
        return !joinedWhileAway.isEmpty() || !baseVisits.isEmpty() || !mail.isEmpty();
    }

    public void clearPending() {
        joinedWhileAway.clear();
        newPlayersWhileAway.clear();
        baseVisits.clear();
        mail.clear();
    }
}
