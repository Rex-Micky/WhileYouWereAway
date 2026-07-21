package dev.rexmicky.wywa;

import org.bukkit.Location;
import org.bukkit.inventory.ItemStack;

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

    /** A piece of mail (or a guestbook note). deliverAt lets letters arrive in the future. */
    public static final class Mail {
        public final String from;
        public final String message;
        public final long time;
        public final long deliverAt;   // 0 = deliver on next login; else not until this epoch millis
        public Mail(String from, String message, long time) { this(from, message, time, 0L); }
        public Mail(String from, String message, long time, long deliverAt) {
            this.from = from;
            this.message = message;
            this.time = time;
            this.deliverAt = deliverAt;
        }
        public boolean isDue() { return deliverAt <= System.currentTimeMillis(); }
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

    // Daily rewards / login streak (also drives loyalty tier).
    private String lastClaimDate = "";   // ISO yyyy-MM-dd; empty = never claimed
    private int currentStreak;
    private int longestStreak;
    private int totalClaims;

    // Guestbook signed by visitors to this player's base.
    private final List<Mail> guestbook = new ArrayList<>();

    // Gifts left for this player, delivered on their next login.
    private final List<ItemStack> gifts = new ArrayList<>();

    // World handles for this player's loyalty shrine and Watcher companion (nullable).
    private UUID shrineEntity;
    private UUID watcherEntity;
    private int watcherLevel;

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

    public String getLastClaimDate() { return lastClaimDate; }
    public void setLastClaimDate(String d) { this.lastClaimDate = d == null ? "" : d; }
    public int getCurrentStreak() { return currentStreak; }
    public void setCurrentStreak(int s) { this.currentStreak = s; }
    public int getLongestStreak() { return longestStreak; }
    public void setLongestStreak(int s) { this.longestStreak = s; }
    public int getTotalClaims() { return totalClaims; }
    public void setTotalClaims(int t) { this.totalClaims = t; }

    public List<Mail> getGuestbook() { return guestbook; }
    public List<ItemStack> getGifts() { return gifts; }

    public UUID getShrineEntity() { return shrineEntity; }
    public void setShrineEntity(UUID id) { this.shrineEntity = id; }
    public UUID getWatcherEntity() { return watcherEntity; }
    public void setWatcherEntity(UUID id) { this.watcherEntity = id; }
    public int getWatcherLevel() { return watcherLevel; }
    public void setWatcherLevel(int l) { this.watcherLevel = l; }

    public boolean hasAnythingToReport() {
        return !joinedWhileAway.isEmpty() || !baseVisits.isEmpty() || !mail.isEmpty();
    }

    public void clearPending() {
        joinedWhileAway.clear();
        newPlayersWhileAway.clear();
        baseVisits.clear();
        // mail is intentionally NOT cleared here — future-dated letters survive until due.
    }
}
