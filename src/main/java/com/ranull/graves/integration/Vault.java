package com.ranull.graves.integration;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.RegisteredServiceProvider;

/**
 * Provides an integration with Vault's economy system to manage player balances.
 */
public final class Vault {
    private Economy economy;
    private Permission permission;

    /**
     * Constructs a new Vault integration instance with the specified Economy instance.
     *
     * @param economy The Economy instance provided by Vault.
     * @param permission The Permission instance provided by Vault.
     */
    public Vault(Economy economy, Permission permission) {
        this.economy = economy;
        this.permission = permission;
    }

    /**
     * Constructs a new Vault integration instance with the specified Economy instance.
     *
     * @param economy The Economy instance provided by Vault.
     * @deprecated
     */
    @Deprecated
    public Vault(Economy economy) {
        this.economy = economy;
    }

    /**
     * Constructs a new Vault integration instance with the specified Economy instance.
     *
     * @param permission The Permission instance provided by Vault.
     * @deprecated
     */
    @Deprecated
    public Vault(Permission permission) {
        this.permission = permission;
    }

    /**
     * Checks if a player has a balance greater than or equal to the specified amount.
     *
     * @param player  The player whose balance to check.
     * @param balance The amount to check against.
     * @return {@code true} if the player has a balance greater than or equal to the specified amount, otherwise {@code false}.
     */
    public boolean hasBalance(OfflinePlayer player, double balance) {
        return economy.getBalance(player) >= balance;
    }

    /**
     * Gets the current balance of a player.
     *
     * @param player The player whose balance to retrieve.
     * @return The balance of the player.
     */
    public double getBalance(OfflinePlayer player) {
        return economy.getBalance(player);
    }

    /**
     * Withdraws a specified amount from a player's balance.
     *
     * @param player  The player from whom the balance will be withdrawn.
     * @param balance The amount to withdraw.
     * @return {@code true} if the withdrawal was successful, otherwise {@code false}.
     */
    public boolean withdrawBalance(OfflinePlayer player, double balance) {
        return balance <= 0 || economy.withdrawPlayer(player, balance).transactionSuccess();
    }

    /**
     * Checks if a player has the specified permission.
     *
     * @param player The player whose permission to check.
     * @param permissionNode The permission node to check.
     * @return {@code true} if the player has the specified permission, otherwise {@code false}.
     */
    public boolean hasPermission(OfflinePlayer player, String permissionNode) {
        return permission.has((CommandSender) player, permissionNode);
    }
}