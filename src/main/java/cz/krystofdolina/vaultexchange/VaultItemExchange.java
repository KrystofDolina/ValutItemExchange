/*
 * Vault Item Exchange - A Minecraft Paper Plugin
 * Copyright (C) 2025 Kryštof Dolina
 * Licensed under GNU GPL v3.0. Created with AI assistance (Gemini).
 */

package cz.krystofdolina.vaultexchange;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Material;
import org.bukkit.command.*;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VaultItemExchange extends JavaPlugin implements CommandExecutor, TabCompleter {
    private static Economy econ = null;
    private final String GREEN_HEX = "#24ba0d";

    @Override
    public void onEnable() {
        this.saveDefaultConfig();
        
        // Connect to Vault Economy
        if (!setupEconomy()) {
            getLogger().severe("Vault dependency not found! Disabling plugin.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register all commands and their tab-completers
        registerCommand("xsell");
        registerCommand("xwithdraw");
        registerCommand("xitems");
        registerCommand("xmoney");

        getLogger().info("Vault Item Exchange by Kryštof Dolina enabled successfully.");
    }

    private void registerCommand(String name) {
        PluginCommand cmd = getCommand(name);
        if (cmd != null) {
            cmd.setExecutor(this);
            cmd.setTabCompleter(this);
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;
        econ = rsp.getProvider();
        return econ != null;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("Only players can use these commands.");
            return true;
        }

        // Command: /xmoney
        if (label.equalsIgnoreCase("xmoney")) {
            player.sendMessage(ChatColor.of(GREEN_HEX) + "Your balance: " + ChatColor.WHITE + econ.format(econ.getBalance(player)));
            return true;
        }

        // Command: /xitems
        if (label.equalsIgnoreCase("xitems")) {
            handleListItems(player);
            return true;
        }

        // Check arguments for /xsell and /xwithdraw
        if (args.length < 2) return false;

        String materialName = args[0].toUpperCase();
        int amount;
        try {
            amount = Integer.parseInt(args[1]);
            if (amount <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "The amount must be a positive number.");
            return true;
        }

        double pricePerUnit = getConfig().getDouble("prices." + materialName, -1);
        Material material = Material.getMaterial(materialName);

        if (material == null || pricePerUnit == -1) {
            player.sendMessage(ChatColor.RED + "That item is not available for exchange.");
            return true;
        }

        if (label.equalsIgnoreCase("xsell")) {
            handleSell(player, material, amount, pricePerUnit);
        } else if (label.equalsIgnoreCase("xwithdraw")) {
            handleWithdraw(player, material, amount, pricePerUnit);
        }

        return true;
    }

    // --- Tab Completion Logic ---
    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        if (!(sender instanceof Player)) return null;

        // Arg 1: Suggest items from config
        if (args.length == 1 && (alias.equalsIgnoreCase("xsell") || alias.equalsIgnoreCase("xwithdraw"))) {
            ConfigurationSection section = getConfig().getConfigurationSection("prices");
            if (section == null) return new ArrayList<>();

            Set<String> items = section.getKeys(false);
            String input = args[0].toLowerCase();

            return items.stream()
                    .filter(item -> item.toLowerCase().startsWith(input))
                    .collect(Collectors.toList());
        }
        
        // Arg 2: Suggest specific amounts
        if (args.length == 2 && (alias.equalsIgnoreCase("xsell") || alias.equalsIgnoreCase("xwithdraw"))) {
            List<String> suggestions = List.of("1", "2", "3", "5");
            String input = args[1];
            return suggestions.stream()
                    .filter(s -> s.startsWith(input))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }

    private void handleListItems(Player player) {
        ConfigurationSection section = getConfig().getConfigurationSection("prices");
        if (section == null) {
            player.sendMessage(ChatColor.RED + "No items are configured in the exchange.");
            return;
        }
        player.sendMessage(ChatColor.of(GREEN_HEX) + "--- Available Items for Exchange ---");
        for (String key : section.getKeys(false)) {
            player.sendMessage(ChatColor.WHITE + formatName(key) + ": " + ChatColor.of(GREEN_HEX) + section.getDouble(key) + " coins");
        }
        player.sendMessage(ChatColor.of(GREEN_HEX) + "------------------------------------");
    }

    /**
     * Formats names nicely (e.g., DIAMOND_SWORD -> Diamond Sword)
     */
    private String formatName(String n) {
        String[] words = n.toLowerCase().split("_");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }

    private void handleSell(Player player, Material mat, int amt, double price) {
        if (!player.getInventory().containsAtLeast(new ItemStack(mat), amt)) {
            player.sendMessage(ChatColor.RED + "You don't have " + amt + "x " + formatName(mat.name()) + ".");
            return;
        }
        
        player.getInventory().removeItem(new ItemStack(mat, amt));
        double earnings = amt * price;
        econ.depositPlayer(player, earnings);
        
        player.sendMessage(ChatColor.of(GREEN_HEX) + "Sold " + amt + "x " + formatName(mat.name()) + " for " + earnings + " coins.");
    }

    private void handleWithdraw(Player p, Material m, int a, double price) {
        double cost = a * price;
        if (econ.getBalance(p) < cost) {
            p.sendMessage(ChatColor.RED + "You need " + cost + " coins, but you have less! (No debts allowed)");
            return;
        }
        
        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage(ChatColor.RED + "Your inventory is full!");
            return;
        }

        econ.withdrawPlayer(p, cost);
        p.getInventory().addItem(new ItemStack(m, a));
        
        p.sendMessage(ChatColor.of(GREEN_HEX) + "Withdrew " + a + "x " + formatName(m.name()) + " for " + cost + " coins.");
    }
}