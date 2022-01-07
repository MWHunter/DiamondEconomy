package org.abyssmc.diamondeconomy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public final class DiamondEconomy extends JavaPlugin implements CommandExecutor {
    private Economy econ;
    private static int conversionRate = 100;
    private static String prefix = ChatColor.WHITE + "[" + ChatColor.GREEN + "Economy" + ChatColor.WHITE + "] " ;

    public static void removeInventoryItems(PlayerInventory inv, Material type, int amount) {
        for (int i = 0; i < inv.getSize(); i++) {
            ItemStack is = inv.getItem(i);

            if (amount <= 0) return;

            if (is != null && is.getType() == type) {
                if (is.getAmount() < amount) {
                    amount -= is.getAmount();
                    inv.setItem(i, new ItemStack(Material.AIR));
                } else {
                    inv.setItem(i, is.subtract(amount));
                    return;
                }
            }
        }
    }

    @Override
    public void onEnable() {
        // Plugin startup logic
        if (!setupEconomy()) {
            this.getLogger().severe("Disabled due to no Vault dependency found!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player = (Player) sender;

        if (command.getName().equalsIgnoreCase("deposit") && args.length == 0) {
            int amount = 0;
            for (int i = 0; i < 36; i++) {
                ItemStack itemStack = player.getInventory().getItem(i);

                if (itemStack != null && itemStack.getType() == Material.DIAMOND) {
                    amount += itemStack.getAmount();
                }
            }

            player.getInventory().remove(Material.DIAMOND);

            EconomyResponse r = econ.depositPlayer(player, amount * conversionRate);
            if (r.transactionSuccess()) {
                player.sendMessage(prefix + ChatColor.AQUA + "You have added " + ChatColor.WHITE + amount +
                        ChatColor.AQUA + " diamond" + plural(amount) + " to your account");

            } else {
                player.getInventory().addItem(new ItemStack(Material.DIAMOND, amount));
                sender.sendMessage(ChatColor.RED + "An error occurred: " + r.errorMessage);
            }

            return true;
        }

        if (args.length != 1) {
            player.sendMessage(prefix + ChatColor.RED + "You must provide a positive integer to deposit or withdrawal");
            return false;
        }

        try {
            int amount = Integer.parseInt(args[0]);

            if (amount <= 0) {
                player.sendMessage(prefix + ChatColor.RED + "You must provide a positive integer");
                return true;
            }

            if (command.getName().equalsIgnoreCase("deposit")) {
                if (player.getInventory().containsAtLeast(new ItemStack(Material.DIAMOND), amount)) {
                    removeInventoryItems(player.getInventory(), Material.DIAMOND, amount);

                    EconomyResponse r = econ.depositPlayer(player, amount * conversionRate);
                    if (r.transactionSuccess()) {
                        player.sendMessage(prefix + ChatColor.AQUA + "You have added " + ChatColor.WHITE + amount +
                                ChatColor.AQUA + " diamond" + plural(amount) + " to your account");

                    } else {
                        player.getInventory().addItem(new ItemStack(Material.DIAMOND, amount));
                        sender.sendMessage(prefix + r.errorMessage);
                    }

                } else {
                    sender.sendMessage(prefix + ChatColor.RED + "You do not have " + amount + " diamond" + plural(amount) + " in your inventory");
                }

                return true;
            }

            if (command.getName().equalsIgnoreCase("withdraw" +
                    "")) {
                if (econ.has(player, amount)) {
                    int freeDiamondSpace = 0;
                    for (int i = 0; i < 36; i++) {
                        ItemStack itemStack = player.getInventory().getItem(i);

                        if (itemStack == null) {
                            freeDiamondSpace += 64;
                        } else if (itemStack.getType() == Material.DIAMOND) {
                            freeDiamondSpace += (64 - itemStack.getAmount());
                        }
                    }

                    if (freeDiamondSpace > amount) {
                        EconomyResponse r = econ.withdrawPlayer(player, amount * conversionRate);
                        if (r.transactionSuccess()) {
                            player.getInventory().addItem(new ItemStack(Material.DIAMOND, amount));
                            player.sendMessage(prefix + ChatColor.AQUA + "You have withdrawn " + ChatColor.WHITE +
                                    amount + ChatColor.AQUA + " diamond" + plural(amount));
                        } else {
                            sender.sendMessage(prefix + ChatColor.RED + "An error occurred: " + r.errorMessage);
                        }

                    } else {
                        sender.sendMessage(prefix + ChatColor.RED + "You do not have enough inventory space for " + amount + " diamond" + plural(amount));
                    }
                } else {
                    player.sendMessage(prefix + ChatColor.RED + "You do not have " + amount + " diamond" + plural(amount) + " in your account");
                }

                return true;
            }
        } catch (NumberFormatException e) {
            player.sendMessage(prefix + ChatColor.RED + "You must provide a positive integer");
            return true;
        }

        return false;
    }

    private String plural(int amount) {
        if (amount == 1) {
            return "";
        }

        return "s";
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
}
