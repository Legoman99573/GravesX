package com.ranull.graves.integration;

import com.ranull.graves.Graves;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.List;

/**
 * Integrates GravesX with BagOfGold's physical-money items.
 *
 * <p>Only item stacks marked as real BagOfGold/CustomItemsLib rewards are processed.
 * This integration never touches digital balances (Vault/DB).</p>
 */
public final class BagOfGoldPhysicalMoneyIntegration {
    private static final String BAGOFGOLD_PLUGIN = "BagOfGold";
    private static final double EPSILON = 1.0E-9D;
    private static final int FLOOR_SCALE = 5;

    private final Graves plugin;
    private @Nullable RewardReflectionBridge bridge;
    private boolean warnedMissingPlugin;
    private boolean warnedReflectionFailure;
    private boolean loggedHook;

    public BagOfGoldPhysicalMoneyIntegration(@NotNull Graves plugin) {
        this.plugin = plugin;
    }

    /**
     * Splits BagOfGold physical money from grave items into ignored items so ignored items
     * are dropped on the ground by GravesX.
     *
     * @param livingEntity   dead entity
     * @param permissionList resolved GravesX permissions for this entity (nullable)
     * @param graveItems     items currently intended for grave storage
     * @param ignoredItems   items that should drop outside the grave
     * @param preserveSlots  true for EXACT storage mode (keep list indices stable)
     */
    public void splitPhysicalMoney(
            @NotNull LivingEntity livingEntity,
            @Nullable List<String> permissionList,
            @NotNull List<ItemStack> graveItems,
            @NotNull List<ItemStack> ignoredItems,
            boolean preserveSlots
    ) {
        if (!(livingEntity instanceof Player)) return;
        if (graveItems.isEmpty()) return;
        if (!isIntegrationEnabled()) return;

        ConfigurationSection section = plugin.getConfigManager()
                .getConfigSection("physical-money.bagofgold.enabled", livingEntity, permissionList);
        if (section == null) return;

        if (!section.getBoolean("physical-money.bagofgold.enabled", true)) return;
        if (!isAllowedByDeathScope(livingEntity, section.getString("physical-money.bagofgold.apply-on", "pvp_and_pve"))) {
            return;
        }

        RewardReflectionBridge resolved = resolveBridge();
        if (resolved == null) return;

        double dropPercent = getDropPercent(section);
        if (dropPercent <= 0.0D) return;

        double totalMoney = computeTotalPhysicalMoney(graveItems, resolved);
        if (totalMoney <= 0.0D) return;

        double targetDropValue = floorToScale(totalMoney * (dropPercent / 100.0D), FLOOR_SCALE);
        if (targetDropValue <= 0.0D) return;

        double moved = 0.0D;
        for (int i = 0; i < graveItems.size() && moved + EPSILON < targetDropValue; i++) {
            ItemStack stack = graveItems.get(i);
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;

            double unitValue = resolved.getUnitMoney(stack);
            if (unitValue <= 0.0D) continue;

            double remainingTarget = (targetDropValue - moved);
            if (remainingTarget > EPSILON && stack.getAmount() == 1) {
                int maxTakeForSingle = (int) Math.floor((remainingTarget + EPSILON) / unitValue);
                if (maxTakeForSingle <= 0) {
                    double dropValue = floorToScale(Math.min(remainingTarget, unitValue), FLOOR_SCALE);
                    if (dropValue > EPSILON && dropValue + EPSILON < unitValue) {
                        ItemStack droppedPartial = resolved.rewriteMoney(stack, dropValue);
                        ItemStack keptPartial = resolved.rewriteMoney(stack, floorToScale(unitValue - dropValue, FLOOR_SCALE));
                        if (droppedPartial != null && keptPartial != null) {
                            ignoredItems.add(droppedPartial);
                            graveItems.set(i, keptPartial);
                            moved += dropValue;
                            continue;
                        }
                    }
                }
            }

            int maxTake = (int) Math.floor(((targetDropValue - moved) + EPSILON) / unitValue);
            if (maxTake <= 0) continue;

            int available = stack.getAmount();
            int take = Math.min(available, maxTake);
            if (take <= 0) continue;

            if (take >= available) {
                ItemStack dropped = stack.clone();
                ignoredItems.add(dropped);
                if (preserveSlots) {
                    graveItems.set(i, null);
                } else {
                    graveItems.remove(i);
                    i--;
                }
            } else {
                ItemStack dropped = stack.clone();
                dropped.setAmount(take);
                ignoredItems.add(dropped);

                ItemStack remaining = stack.clone();
                remaining.setAmount(available - take);
                graveItems.set(i, remaining);
            }

            moved += (unitValue * take);
        }

        boolean forceMinimumDrop = section.getBoolean("physical-money.bagofgold.force-minimum-drop-item", true);
        if (forceMinimumDrop && targetDropValue > 0.0D && moved <= EPSILON) {
            moved += forceSingleUnitDrop(graveItems, ignoredItems, preserveSlots, resolved);
        }

        if (moved > 0.0D) {
            plugin.debugMessage(
                    "BagOfGold physical split applied: moved " + floorToScale(moved, FLOOR_SCALE)
                            + " value to ground drops (" + dropPercent + "% target).",
                    1
            );
        }
    }

    private boolean isIntegrationEnabled() {
        FileConfiguration config = plugin.getConfig();
        if (config == null) return true;

        String path = "settings.integration.bagofgold.enabled";
        if (config.contains(path) && !config.getBoolean(path, true)) return false;

        return true;
    }

    private double forceSingleUnitDrop(
            @NotNull List<ItemStack> graveItems,
            @NotNull List<ItemStack> ignoredItems,
            boolean preserveSlots,
            @NotNull RewardReflectionBridge resolved
    ) {
        int bestIndex = -1;
        double bestUnitValue = Double.MAX_VALUE;

        for (int i = 0; i < graveItems.size(); i++) {
            ItemStack stack = graveItems.get(i);
            if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) continue;

            double unitValue = resolved.getUnitMoney(stack);
            if (unitValue <= 0.0D) continue;

            if (unitValue < bestUnitValue) {
                bestUnitValue = unitValue;
                bestIndex = i;
            }
        }

        if (bestIndex < 0) return 0.0D;

        ItemStack stack = graveItems.get(bestIndex);
        if (stack == null || stack.getType() == Material.AIR || stack.getAmount() <= 0) return 0.0D;

        ItemStack dropped = stack.clone();
        dropped.setAmount(1);
        ignoredItems.add(dropped);

        if (stack.getAmount() <= 1) {
            if (preserveSlots) {
                graveItems.set(bestIndex, null);
            } else {
                graveItems.remove(bestIndex);
            }
        } else {
            ItemStack remaining = stack.clone();
            remaining.setAmount(stack.getAmount() - 1);
            graveItems.set(bestIndex, remaining);
        }

        return bestUnitValue;
    }

    private boolean isAllowedByDeathScope(@NotNull LivingEntity livingEntity, @Nullable String modeRaw) {
        String mode = modeRaw == null ? "pvp_and_pve" : modeRaw.trim().toLowerCase();
        boolean pvp = livingEntity.getKiller() != null;

        return switch (mode) {
            case "pvp_only" -> pvp;
            case "pve_only" -> !pvp;
            default -> true; // pvp_and_pve (or unknown value -> safe default)
        };
    }

    private double getDropPercent(@NotNull ConfigurationSection section) {
        final String dropPath = "physical-money.bagofgold.drop-on-ground-percent";
        final String keepPath = "physical-money.bagofgold.keep-in-grave-percent";

        double drop = section.contains(dropPath)
                ? section.getDouble(dropPath, 30.0D)
                : (100.0D - section.getDouble(keepPath, 70.0D));

        if (!Double.isFinite(drop)) return 0.0D;
        if (drop < 0.0D) return 0.0D;
        if (drop > 100.0D) return 100.0D;
        return drop;
    }

    private double computeTotalPhysicalMoney(@NotNull List<ItemStack> items, @NotNull RewardReflectionBridge resolved) {
        double total = 0.0D;
        for (ItemStack item : items) {
            if (item == null || item.getType() == Material.AIR || item.getAmount() <= 0) continue;
            double unit = resolved.getUnitMoney(item);
            if (unit <= 0.0D) continue;
            total += unit * item.getAmount();
        }
        return floorToScale(total, FLOOR_SCALE);
    }

    private @Nullable RewardReflectionBridge resolveBridge() {
        if (bridge != null) return bridge;

        Plugin bag = plugin.getServer().getPluginManager().getPlugin(BAGOFGOLD_PLUGIN);
        if (bag == null || !bag.isEnabled()) {
            if (!warnedMissingPlugin) {
                warnedMissingPlugin = true;
                plugin.debugMessage("BagOfGold physical-money split is enabled, but BagOfGold is not loaded. Skipping split.", 2);
            }
            return null;
        }

        try {
            bridge = new RewardReflectionBridge();
            if (!loggedHook) {
                loggedHook = true;
                plugin.integrationMessage("Hooked into BagOfGold physical item metadata via CustomItemsLib Reward reflection.");
            }
            return bridge;
        } catch (Throwable throwable) {
            if (!warnedReflectionFailure) {
                warnedReflectionFailure = true;
                plugin.integrationMessage("Failed to initialize BagOfGold physical-money reflection bridge: " + throwable.getMessage(), "warn");
            }
            return null;
        }
    }

    private static double floorToScale(double value, int scale) {
        if (!Double.isFinite(value)) return 0.0D;
        double factor = Math.pow(10, scale);
        return Math.floor(value * factor) / factor;
    }

    private static final class RewardReflectionBridge {
        private final Method rewardIsReward;
        private final Method rewardGetReward;
        private final Method rewardIsMoney;
        private final Method rewardGetMoney;
        private final Method rewardSetMoney;
        private final Method rewardSetDisplayAndLore;
        private final @Nullable Method rewardCheckHash;

        private RewardReflectionBridge() throws ClassNotFoundException, NoSuchMethodException {
            Class<?> rewardClass = Class.forName("one.lindegaard.CustomItemsLib.rewards.Reward");
            this.rewardIsReward = rewardClass.getMethod("isReward", ItemStack.class);
            this.rewardGetReward = rewardClass.getMethod("getReward", ItemStack.class);
            this.rewardIsMoney = rewardClass.getMethod("isMoney");
            this.rewardGetMoney = rewardClass.getMethod("getMoney");
            this.rewardSetMoney = rewardClass.getMethod("setMoney", double.class);
            this.rewardSetDisplayAndLore = rewardClass.getMethod("setDisplayNameAndHiddenLores", ItemStack.class, rewardClass);

            Method checkHash;
            try {
                checkHash = rewardClass.getMethod("checkHash");
            } catch (NoSuchMethodException ignored) {
                checkHash = null;
            }
            this.rewardCheckHash = checkHash;
        }

        private double getUnitMoney(@Nullable ItemStack itemStack) {
            if (itemStack == null || itemStack.getType() == Material.AIR) return 0.0D;

            try {
                Object isReward = rewardIsReward.invoke(null, itemStack);
                if (!(isReward instanceof Boolean) || !((Boolean) isReward)) return 0.0D;

                Object rewardObj = rewardGetReward.invoke(null, itemStack);
                if (rewardObj == null) return 0.0D;

                if (rewardCheckHash != null) {
                    Object checkHash = rewardCheckHash.invoke(rewardObj);
                    if (!(checkHash instanceof Boolean) || !((Boolean) checkHash)) return 0.0D;
                }

                Object isMoney = rewardIsMoney.invoke(rewardObj);
                if (!(isMoney instanceof Boolean) || !((Boolean) isMoney)) return 0.0D;

                Object money = rewardGetMoney.invoke(rewardObj);
                if (!(money instanceof Number)) return 0.0D;

                double unitValue = ((Number) money).doubleValue();
                return (Double.isFinite(unitValue) && unitValue > 0.0D) ? unitValue : 0.0D;
            } catch (Throwable ignored) {
                return 0.0D;
            }
        }

        private @Nullable ItemStack rewriteMoney(@Nullable ItemStack base, double newMoney) {
            if (base == null || base.getType() == Material.AIR) return null;
            if (!Double.isFinite(newMoney) || newMoney <= 0.0D) return null;

            try {
                Object isReward = rewardIsReward.invoke(null, base);
                if (!(isReward instanceof Boolean) || !((Boolean) isReward)) return null;

                Object rewardObj = rewardGetReward.invoke(null, base);
                if (rewardObj == null) return null;

                Object isMoney = rewardIsMoney.invoke(rewardObj);
                if (!(isMoney instanceof Boolean) || !((Boolean) isMoney)) return null;

                rewardSetMoney.invoke(rewardObj, newMoney);

                ItemStack rewritten = (ItemStack) rewardSetDisplayAndLore.invoke(null, base.clone(), rewardObj);
                if (rewritten == null || rewritten.getType() == Material.AIR) return null;
                rewritten.setAmount(1);
                return rewritten;
            } catch (Throwable ignored) {
                return null;
            }
        }
    }
}
