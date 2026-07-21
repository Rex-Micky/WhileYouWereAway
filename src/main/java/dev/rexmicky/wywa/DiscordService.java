package dev.rexmicky.wywa;

import org.bukkit.Bukkit;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Pattern;

/**
 * Sends base-intrusion alerts to Discord, so a player gets a real-world ping while
 * they're offline. Each player may set their own webhook, or the server can use one.
 */
public class DiscordService {

    /**
     * Only genuine Discord webhook endpoints are accepted.
     * Players supply these themselves, so allowing arbitrary URLs would let someone
     * point the server at an internal address and use it as an SSRF probe.
     */
    private static final Pattern WEBHOOK = Pattern.compile(
            "^https://(?:canary\\.|ptb\\.)?discord(?:app)?\\.com/api/webhooks/\\d+/[A-Za-z0-9_.-]+$");

    private final WhileYouWereAwayPlugin plugin;
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public DiscordService(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean isValidWebhook(String url) {
        return url != null && WEBHOOK.matcher(url.trim()).matches();
    }

    public boolean enabled() {
        return plugin.getConfig().getBoolean("discord.enabled", false);
    }

    /** Fire an alert for this player — their own webhook if set, else the server's. */
    public void alert(PlayerData owner, String content) {
        if (!enabled()) return;
        String url = owner.getDiscordWebhook();
        if (url == null || url.isBlank()) url = plugin.getConfig().getString("discord.webhook-url", "");
        if (!isValidWebhook(url)) return;

        final String target = url.trim();
        final String body = "{\"content\":\"" + escape(content) + "\"}";
        // Never touch the network on the main thread.
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> post(target, body));
    }

    private void post(String url, String json) {
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(8))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "WhileYouWereAway/1.0")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 300) {
                plugin.getLogger().warning("Discord webhook returned HTTP " + response.statusCode());
            }
        } catch (Exception ex) {
            plugin.getLogger().warning("Discord webhook failed: " + ex.getMessage());
        }
    }

    /**
     * JSON-escape, and defuse mass pings — a visitor's name should never be able to
     * make the plugin ping @everyone in someone's server.
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("@everyone", "@​everyone")
                .replace("@here", "@​here");
    }
}
