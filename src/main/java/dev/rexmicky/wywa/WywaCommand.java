package dev.rexmicky.wywa;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class WywaCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList(
            "home", "daily", "streak", "chronicle", "guestbook", "sign", "gift",
            "watcher", "tour", "setbase", "base", "mail", "letter", "futureself", "reload");

    private static final Pattern DURATION = Pattern.compile("(?i)(\\d+)([smhdw])");

    private final WhileYouWereAwayPlugin plugin;

    public WywaCommand(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "home" : args[0].toLowerCase();
        switch (sub) {
            case "home" -> {
                if (sender instanceof Player p) plugin.getHomeGui().open(p); else sender.sendMessage("Players only.");
            }
            case "setbase" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                PlayerData d = plugin.getStore().get(p.getUniqueId());
                d.setName(p.getName());
                d.setBase(p.getLocation());
                plugin.getLoyalty().updateShrine(d);
                plugin.getStore().save();
                plugin.send(p, "base-set");
                sendBase(p, d);
            }
            case "base" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                PlayerData d = plugin.getStore().get(p.getUniqueId());
                if (d.hasBase()) sendBase(p, d); else plugin.send(p, "no-base");
            }
            case "daily", "claim" -> onlyPlayer(sender, p -> plugin.getDaily().claim(p));
            case "streak" -> onlyPlayer(sender, p -> plugin.getDaily().streakInfo(p));
            case "chronicle" -> onlyPlayer(sender, p -> plugin.getChronicle().reopen(p));
            case "guestbook" -> onlyPlayer(sender, p -> plugin.getGuestbook().view(p));
            case "watcher" -> onlyPlayer(sender, p -> plugin.getWatcher().summon(p));
            case "tour" -> onlyPlayer(sender, p -> plugin.getTour().start(p));
            case "sign" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (args.length < 2) { plugin.send(p, "sign-usage"); return true; }
                plugin.getGuestbook().sign(p, String.join(" ", Arrays.copyOfRange(args, 1, args.length)));
            }
            case "gift" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                if (args.length < 2) { plugin.send(p, "gift-usage"); return true; }
                plugin.getGuestbook().gift(p, args[1]);
            }
            case "mail" -> doMail(sender, args);
            case "letter" -> doLetter(sender, args);
            case "futureself" -> doFutureSelf(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("wywa.admin")) { plugin.send(sender, "usage"); return true; }
                plugin.reloadConfig();
                plugin.send(sender, "reloaded");
            }
            default -> plugin.send(sender, "usage");
        }
        return true;
    }

    private void onlyPlayer(CommandSender sender, java.util.function.Consumer<Player> action) {
        if (sender instanceof Player p) action.accept(p); else sender.sendMessage("Players only.");
    }

    private void doMail(CommandSender sender, String[] args) {
        if (args.length < 3) { plugin.send(sender, "mail-usage"); return; }
        String target = args[1];
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        String from = sender instanceof Player p ? p.getName() : "Console";
        if (from.equalsIgnoreCase(target)) { plugin.send(sender, "mail-self"); return; }

        Player online = Bukkit.getPlayerExact(target);
        if (online != null) {
            online.sendMessage(plugin.format("prefix") + plugin.format("mail-line", "from", from, "message", message));
            plugin.send(sender, "mail-sent", "player", online.getName());
            return;
        }
        queueMail(target, from, message, 0L);
        plugin.send(sender, "mail-queued", "player", target);
    }

    private void doLetter(CommandSender sender, String[] args) {
        if (args.length < 4) { plugin.send(sender, "letter-usage"); return; }
        String target = args[1];
        long delay = parseDuration(args[2]);
        if (delay <= 0) { plugin.send(sender, "bad-duration"); return; }
        String message = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
        String from = sender instanceof Player p ? p.getName() : "Console";
        queueMail(target, from, message, System.currentTimeMillis() + delay);
        plugin.send(sender, "letter-scheduled", "player", target, "when", args[2]);
    }

    private void doFutureSelf(CommandSender sender, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return; }
        if (args.length < 3) { plugin.send(p, "futureself-usage"); return; }
        long delay = parseDuration(args[1]);
        if (delay <= 0) { plugin.send(p, "bad-duration"); return; }
        String message = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        PlayerData d = plugin.getStore().get(p.getUniqueId());
        d.getMail().add(new PlayerData.Mail(p.getName() + " (past you)", message,
                System.currentTimeMillis(), System.currentTimeMillis() + delay));
        plugin.getStore().save();
        plugin.send(p, "futureself-scheduled", "when", args[1]);
    }

    private void queueMail(String targetName, String from, String message, long deliverAt) {
        OfflinePlayer off = Bukkit.getOfflinePlayer(targetName);
        PlayerData d = plugin.getStore().get(off.getUniqueId());
        d.setName(targetName);
        d.getMail().add(new PlayerData.Mail(from, message, System.currentTimeMillis(), deliverAt));
        plugin.getStore().save();
    }

    private long parseDuration(String input) {
        Matcher m = DURATION.matcher(input);
        long seconds = 0;
        int matched = 0;
        while (m.find()) {
            matched += m.group().length();
            long v = Long.parseLong(m.group(1));
            switch (Character.toLowerCase(m.group(2).charAt(0))) {
                case 'w' -> seconds += v * 604800L;
                case 'd' -> seconds += v * 86400L;
                case 'h' -> seconds += v * 3600L;
                case 'm' -> seconds += v * 60L;
                case 's' -> seconds += v;
            }
        }
        return matched == input.length() && seconds > 0 ? seconds * 1000L : -1;
    }

    private void sendBase(Player p, PlayerData d) {
        plugin.sendRaw(p, "base-info",
                "world", d.getBaseWorld(),
                "x", (int) d.getBx(), "y", (int) d.getBy(), "z", (int) d.getBz());
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return SUBS.stream().filter(x -> x.startsWith(args[0].toLowerCase())).collect(Collectors.toList());
        }
        if (args.length == 2 && Arrays.asList("mail", "letter", "gift").contains(args[0].toLowerCase())) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
