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
import java.util.stream.Collectors;

public class WywaCommand implements CommandExecutor, TabCompleter {

    private static final List<String> SUBS = Arrays.asList("setbase", "mail", "base", "reload");

    private final WhileYouWereAwayPlugin plugin;

    public WywaCommand(WhileYouWereAwayPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length == 0 ? "help" : args[0].toLowerCase();
        switch (sub) {
            case "setbase" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                PlayerData d = plugin.getStore().get(p.getUniqueId());
                d.setName(p.getName());
                d.setBase(p.getLocation());
                plugin.getStore().save();
                plugin.send(p, "base-set");
                sendBase(p, d);
            }
            case "base" -> {
                if (!(sender instanceof Player p)) { sender.sendMessage("Players only."); return true; }
                PlayerData d = plugin.getStore().get(p.getUniqueId());
                if (d.hasBase()) sendBase(p, d); else plugin.send(p, "no-base");
            }
            case "mail" -> doMail(sender, args);
            case "reload" -> {
                if (!sender.hasPermission("wywa.admin")) { plugin.send(sender, "usage"); return true; }
                plugin.reloadConfig();
                plugin.send(sender, "reloaded");
            }
            default -> plugin.send(sender, "usage");
        }
        return true;
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
        OfflinePlayer off = Bukkit.getOfflinePlayer(target);
        PlayerData d = plugin.getStore().get(off.getUniqueId());
        d.setName(target);
        d.getMail().add(new PlayerData.Mail(from, message, System.currentTimeMillis()));
        plugin.getStore().save();
        plugin.send(sender, "mail-queued", "player", target);
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
        if (args.length == 2 && args[0].equalsIgnoreCase("mail")) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName)
                    .filter(n -> n.toLowerCase().startsWith(args[1].toLowerCase())).collect(Collectors.toList());
        }
        return List.of();
    }
}
