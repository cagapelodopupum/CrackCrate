package com.crackcrate.commands;

import com.crackcrate.CrackCrate;
import com.crackcrate.managers.CrateManager;
import com.crackcrate.models.CrateReward;
import com.crackcrate.models.CrateType;
import com.crackcrate.utils.ColorUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.stream.Collectors;

public class CrateCommand implements CommandExecutor, TabCompleter {

    private final CrackCrate plugin;
    private final CrateManager crateManager;

    public CrateCommand(CrackCrate plugin) {
        this.plugin = plugin;
        this.crateManager = plugin.getCrateManager();
    }

    private String prefix() {
        return ColorUtils.colorize(plugin.getConfig().getString("messages.prefix", "&8[&6CrackCrate&8] &r"));
    }

    private void msg(CommandSender s, String key) {
        s.sendMessage(prefix() + ColorUtils.colorize(plugin.getConfig().getString("messages." + key, key)));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {

        // /givecrate <player> <crate> [amount]
        if (command.getName().equalsIgnoreCase("givecrate")) {
            if (!sender.hasPermission("crackcrate.give")) { msg(sender, "no-permission"); return true; }
            if (args.length < 2) {
                sender.sendMessage(prefix() + "&cUso: /givecrate <jogador> <crate> [quantidade]");
                return true;
            }
            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) { sender.sendMessage(prefix() + "&cJogador não encontrado!"); return true; }
            CrateType crate = crateManager.getCrate(args[1]);
            if (crate == null) {
                sender.sendMessage(prefix() + ColorUtils.colorize(
                        plugin.getConfig().getString("messages.invalid-crate", "").replace("%crate%", args[1])));
                return true;
            }
            int amount = 1;
            if (args.length >= 3) {
                try { amount = Math.max(1, Integer.parseInt(args[2])); } catch (NumberFormatException ignored) {}
            }
            ItemStack key = crateManager.createKey(crate, amount);
            target.getInventory().addItem(key);

            String msgGiven = plugin.getConfig().getString("messages.key-given", "")
                    .replace("%crate%", crate.getDisplayName())
                    .replace("%player%", target.getName())
                    .replace("%amount%", String.valueOf(amount));
            sender.sendMessage(prefix() + ColorUtils.colorize(msgGiven));

            String msgReceived = plugin.getConfig().getString("messages.key-received", "")
                    .replace("%crate%", crate.getDisplayName())
                    .replace("%amount%", String.valueOf(amount));
            target.sendMessage(prefix() + ColorUtils.colorize(msgReceived));
            return true;
        }

        // /keyall <crate> <quantidade>
        if (command.getName().equalsIgnoreCase("keyall")) {
            if (!sender.hasPermission("crackcrate.admin")) { msg(sender, "no-permission"); return true; }
            if (args.length < 2) {
                sender.sendMessage(prefix() + "&cUso: /keyall <crate> <quantidade>");
                return true;
            }
            CrateType crate = crateManager.getCrate(args[0]);
            if (crate == null) {
                sender.sendMessage(prefix() + ColorUtils.colorize(
                        plugin.getConfig().getString("messages.invalid-crate", "").replace("%crate%", args[0])));
                return true;
            }
            int amount = 1;
            try { amount = Math.max(1, Integer.parseInt(args[1])); } catch (NumberFormatException ignored) {}
            int finalAmount = amount;
            CrateType finalCrate = crate;
            for (Player online : Bukkit.getOnlinePlayers()) {
                online.getInventory().addItem(crateManager.createKey(finalCrate, finalAmount));
                String msgRec = plugin.getConfig().getString("messages.key-received", "")
                        .replace("%crate%", finalCrate.getDisplayName())
                        .replace("%amount%", String.valueOf(finalAmount));
                online.sendMessage(prefix() + ColorUtils.colorize(msgRec));
            }
            sender.sendMessage(prefix() + ColorUtils.colorize("&aChave &e" + finalCrate.getDisplayName()
                    + " &a(x" + finalAmount + ") enviada para todos os jogadores online!"));
            return true;
        }

        // /crate ...
        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "open": {
                if (!(sender instanceof Player)) { sender.sendMessage("Somente jogadores!"); return true; }
                Player player = (Player) sender;
                if (args.length < 2) { player.sendMessage(prefix() + "&cUso: /crate open <crate>"); return true; }
                CrateType crate = crateManager.getCrate(args[1]);
                if (crate == null) {
                    player.sendMessage(prefix() + ColorUtils.colorize(
                            plugin.getConfig().getString("messages.invalid-crate","").replace("%crate%",args[1])));
                    return true;
                }
                crateManager.openCrate(player, crate);
                break;
            }
            case "give": {
                if (!sender.hasPermission("crackcrate.give")) { msg(sender, "no-permission"); return true; }
                if (args.length < 3) { sender.sendMessage(prefix() + "&cUso: /crate give <jogador> <crate> [qtd]"); return true; }
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) { sender.sendMessage(prefix() + "&cJogador não encontrado!"); return true; }
                CrateType crate = crateManager.getCrate(args[2]);
                if (crate == null) {
                    sender.sendMessage(prefix() + ColorUtils.colorize(
                            plugin.getConfig().getString("messages.invalid-crate","").replace("%crate%",args[2])));
                    return true;
                }
                int amount = 1;
                if (args.length >= 4) { try { amount = Math.max(1,Integer.parseInt(args[3])); } catch (NumberFormatException ignored){} }
                target.getInventory().addItem(crateManager.createKey(crate, amount));

                String msgGiven = plugin.getConfig().getString("messages.key-given","")
                        .replace("%crate%",crate.getDisplayName())
                        .replace("%player%",target.getName())
                        .replace("%amount%",String.valueOf(amount));
                sender.sendMessage(prefix() + ColorUtils.colorize(msgGiven));

                String msgRec = plugin.getConfig().getString("messages.key-received","")
                        .replace("%crate%",crate.getDisplayName())
                        .replace("%amount%",String.valueOf(amount));
                target.sendMessage(prefix() + ColorUtils.colorize(msgRec));
                break;
            }
            case "list": {
                if (!sender.hasPermission("crackcrate.admin")) { msg(sender, "no-permission"); return true; }
                sender.sendMessage(prefix() + "&eCrates disponíveis:");
                crateManager.getCrateTypes().forEach((id, crate) -> {
                    String locStr = crate.getLocation() == null ? "&cnão definida" :
                            "&a" + crate.getLocation().getWorld().getName() + " " +
                            crate.getLocation().getBlockX() + "," +
                            crate.getLocation().getBlockY() + "," +
                            crate.getLocation().getBlockZ();
                    sender.sendMessage(ColorUtils.colorize("  &7- &e" + id + " &7(" + crate.getDisplayName() + "&7) Loc: " + locStr));
                    sender.sendMessage(ColorUtils.colorize("    &7Recompensas: &f" + crate.getRewards().size()));
                });
                break;
            }
            case "rewards": {
                if (!sender.hasPermission("crackcrate.admin")) { msg(sender, "no-permission"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix() + "&cUso: /crate rewards <crate>"); return true; }
                CrateType crate = crateManager.getCrate(args[1]);
                if (crate == null) {
                    sender.sendMessage(prefix() + ColorUtils.colorize(
                            plugin.getConfig().getString("messages.invalid-crate","").replace("%crate%",args[1])));
                    return true;
                }
                sender.sendMessage(prefix() + "&eRecompensas de " + crate.getDisplayName() + "&e:");
                for (CrateReward r : crate.getRewards()) {
                    sender.sendMessage(ColorUtils.colorize("  &7" + r.getId() + " &f- " + r.getDisplayName() +
                            " &7(" + r.getMaterial().name() + " x" + r.getAmount() + ") &6" + r.getChance() + "%"));
                }
                break;
            }
            case "setlocation": {
                if (!(sender instanceof Player)) { sender.sendMessage("Somente jogadores!"); return true; }
                if (!sender.hasPermission("crackcrate.admin")) { msg(sender, "no-permission"); return true; }
                if (args.length < 2) { sender.sendMessage(prefix() + "&cUso: /crate setlocation <crate>"); return true; }
                CrateType crate = crateManager.getCrate(args[1]);
                if (crate == null) {
                    sender.sendMessage(prefix() + ColorUtils.colorize(
                            plugin.getConfig().getString("messages.invalid-crate","").replace("%crate%",args[1])));
                    return true;
                }
                Player player = (Player) sender;
                crateManager.saveLocation(crate, player.getLocation().getBlock().getLocation());
                String locMsg = plugin.getConfig().getString("messages.location-set","")
                        .replace("%crate%",crate.getDisplayName());
                sender.sendMessage(prefix() + ColorUtils.colorize(locMsg));
                break;
            }
            case "reload": {
                if (!sender.hasPermission("crackcrate.reload")) { msg(sender, "no-permission"); return true; }
                plugin.reloadConfig();
                crateManager.loadCrates();
                msg(sender, "reload-done");
                break;
            }
            default:
                sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(ColorUtils.colorize("&8&m----&r &6CrackCrate &8&m----"));
        sender.sendMessage(ColorUtils.colorize("&e/crate open <crate> &7- Abre uma crate"));
        sender.sendMessage(ColorUtils.colorize("&e/crate give <jogador> <crate> [qtd] &7- Dá chave"));
        sender.sendMessage(ColorUtils.colorize("&e/crate list &7- Lista crates"));
        sender.sendMessage(ColorUtils.colorize("&e/crate rewards <crate> &7- Ver recompensas"));
        sender.sendMessage(ColorUtils.colorize("&e/crate setlocation <crate> &7- Define local da crate"));
        sender.sendMessage(ColorUtils.colorize("&e/crate reload &7- Recarrega config"));
        sender.sendMessage(ColorUtils.colorize("&e/givecrate <jogador> <crate> [qtd] &7- Atalho dar chave"));
        sender.sendMessage(ColorUtils.colorize("&e/keyall <crate> <qtd> &7- Dá chave para todos online"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> crateIds = new ArrayList<>(crateManager.getCrateTypes().keySet());

        if (command.getName().equalsIgnoreCase("keyall")) {
            if (args.length == 1) return crateIds;
            return Collections.emptyList();
        }

        if (command.getName().equalsIgnoreCase("givecrate")) {
            if (args.length == 1) return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            if (args.length == 2) return crateIds;
            return Collections.emptyList();
        }

        if (args.length == 1) return Arrays.asList("open", "give", "list", "rewards", "setlocation", "reload");
        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "open": case "setlocation": case "rewards": return crateIds;
                case "give": return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("give")) return crateIds;
        return Collections.emptyList();
    }
}
