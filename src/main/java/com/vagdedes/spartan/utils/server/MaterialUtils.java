package com.vagdedes.spartan.utils.server;

import com.vagdedes.spartan.functionality.important.MultiVersion;
import com.vagdedes.spartan.objects.replicates.SpartanBlock;
import com.vagdedes.spartan.objects.replicates.SpartanInventory;
import com.vagdedes.spartan.objects.replicates.SpartanPlayer;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerData;
import com.vagdedes.spartan.utils.java.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class MaterialUtils {

    private static final Map<String, Material> alternative = new LinkedHashMap<>(60);
    private static final Map<Material, Double> baseMultiplier = new LinkedHashMap<>((7 * 5) + 1);
    private static final double specialMultiplier = 1.5;

    static {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            alternative.put("water", Material.WATER);
            alternative.put("lava", Material.LAVA);
            alternative.put("web", Material.COBWEB);
            alternative.put("gold_axe", Material.GOLDEN_AXE);
            alternative.put("wood_axe", Material.WOODEN_AXE);
            alternative.put("gold_pickaxe", Material.GOLDEN_PICKAXE);
            alternative.put("wood_pickaxe", Material.WOODEN_PICKAXE);
            alternative.put("gold_sword", Material.GOLDEN_SWORD);
            alternative.put("wood_sword", Material.WOODEN_SWORD);
            alternative.put("watch", Material.CLOCK);
            alternative.put("exp_bottle", Material.EXPERIENCE_BOTTLE);
            alternative.put("redstone_comparator", Material.COMPARATOR);
            alternative.put("cake", Material.CAKE);
            alternative.put("diamond_spade", Material.DIAMOND_HOE);
            alternative.put("iron_spade", Material.IRON_HOE);
            alternative.put("gold_spade", Material.GOLDEN_HOE);
            alternative.put("stone_spade", Material.STONE_HOE);
            alternative.put("wood_spade", Material.WOODEN_HOE);
            alternative.put("beetroot_block", Material.BEETROOT);
            alternative.put("magma", Material.MAGMA_BLOCK);
            alternative.put("firework", Material.FIREWORK_ROCKET);
            alternative.put("nether_portal", Material.NETHER_PORTAL);
            alternative.put("cobblestone_wall", Material.COBBLESTONE_WALL);
            alternative.put("end_portal_frame", Material.END_PORTAL_FRAME);
            alternative.put("iron_bars", Material.IRON_BARS);
            alternative.put("enchanting_table", Material.ENCHANTING_TABLE);
            alternative.put("piston_extension", Material.PISTON_HEAD);
            alternative.put("piston_moving", Material.MOVING_PISTON);
            alternative.put("piston", Material.PISTON);
            alternative.put("sticky_piston", Material.STICKY_PISTON);
            alternative.put("lily_pad", Material.LILY_PAD);
            alternative.put("repeater_on", Material.REPEATER);
            alternative.put("repeater_off", Material.REPEATER);
            alternative.put("comparator_on", Material.COMPARATOR);
            alternative.put("comparator_off", Material.COMPARATOR);
            alternative.put("soil", Material.FARMLAND);
            alternative.put("gold_boots", Material.GOLDEN_BOOTS);
            alternative.put("grass_block", Material.GRASS_BLOCK);
            alternative.put("diamond_shovel", Material.DIAMOND_SHOVEL);
            alternative.put("gold_shovel", Material.GOLDEN_SHOVEL);
            alternative.put("iron_shovel", Material.IRON_SHOVEL);
            alternative.put("stone_shovel", Material.STONE_SHOVEL);
            alternative.put("wood_shovel", Material.WOODEN_SHOVEL);
            alternative.put("redstone_torch", Material.REDSTONE_TORCH);
            alternative.put("daylight_detector_1", Material.DAYLIGHT_DETECTOR);
            alternative.put("daylight_detector_2", Material.DAYLIGHT_DETECTOR);
            alternative.put("crafting_table", Material.CRAFTING_TABLE);
            alternative.put("furnace", Material.FURNACE);
            alternative.put("snowball", Material.SNOWBALL);
            alternative.put("fireball", Material.FIRE_CHARGE);
            alternative.put("lead", Material.LEAD);
            alternative.put("carrot_on_a_stick", Material.CARROT_ON_A_STICK);
        } else {
            alternative.put("water", Material.getMaterial("STATIONARY_WATER"));
            alternative.put("lava", Material.getMaterial("STATIONARY_LAVA"));
            alternative.put("web", Material.getMaterial("WEB"));
            alternative.put("gold_axe", Material.getMaterial("GOLD_AXE"));
            alternative.put("wood_axe", Material.getMaterial("WOOD_AXE"));
            alternative.put("gold_pickaxe", Material.getMaterial("GOLD_PICKAXE"));
            alternative.put("wood_pickaxe", Material.getMaterial("WOOD_PICKAXE"));
            alternative.put("gold_sword", Material.getMaterial("GOLD_SWORD"));
            alternative.put("wood_sword", Material.getMaterial("WOOD_SWORD"));
            alternative.put("watch", Material.getMaterial("WATCH"));
            alternative.put("exp_bottle", Material.getMaterial("EXP_BOTTLE"));
            alternative.put("redstone_comparator", Material.getMaterial("REDSTONE_COMPARATOR"));
            alternative.put("cake", Material.getMaterial("CAKE_BLOCK"));
            alternative.put("diamond_spade", Material.getMaterial("DIAMOND_SPADE"));
            alternative.put("iron_spade", Material.getMaterial("IRON_SPADE"));
            alternative.put("gold_spade", Material.getMaterial("GOLD_SPADE"));
            alternative.put("stone_spade", Material.getMaterial("STONE_SPADE"));
            alternative.put("wood_spade", Material.getMaterial("WOOD_SPADE"));
            alternative.put("beetroot_block", Material.getMaterial("BEETROOT_BLOCK"));
            alternative.put("magma", Material.getMaterial("MAGMA"));
            alternative.put("firework", Material.getMaterial("FIREWORK"));
            alternative.put("nether_portal", Material.getMaterial("PORTAL"));
            alternative.put("cobblestone_wall", Material.getMaterial("COBBLE_WALL"));
            alternative.put("end_portal_frame", Material.getMaterial("ENDER_PORTAL_FRAME"));
            alternative.put("iron_bars", Material.getMaterial("IRON_FENCE"));
            alternative.put("enchanting_table", Material.getMaterial("ENCHANTMENT_TABLE"));
            alternative.put("piston_extension", Material.getMaterial("PISTON_EXTENSION"));
            alternative.put("piston_moving", Material.getMaterial("PISTON_MOVING_PIECE"));
            alternative.put("piston", Material.getMaterial("PISTON_BASE"));
            alternative.put("sticky_piston", Material.getMaterial("PISTON_STICKY_BASE"));
            alternative.put("lily_pad", Material.getMaterial("WATER_LILY"));
            alternative.put("repeater_on", Material.getMaterial("DIODE_BLOCK_ON"));
            alternative.put("repeater_off", Material.getMaterial("DIODE_BLOCK_OFF"));
            alternative.put("comparator_on", Material.getMaterial("REDSTONE_COMPARATOR_ON"));
            alternative.put("comparator_off", Material.getMaterial("REDSTONE_COMPARATOR_OFF"));
            alternative.put("soil", Material.getMaterial("SOIL"));
            alternative.put("gold_boots", Material.getMaterial("GOLD_BOOTS"));
            alternative.put("grass_block", Material.getMaterial("GRASS"));
            alternative.put("diamond_shovel", Material.getMaterial("DIAMOND_SPADE"));
            alternative.put("gold_shovel", Material.getMaterial("GOLD_SPADE"));
            alternative.put("iron_shovel", Material.getMaterial("IRON_SPADE"));
            alternative.put("stone_shovel", Material.getMaterial("STONE_SPADE"));
            alternative.put("wood_shovel", Material.getMaterial("WOOD_SPADE"));
            alternative.put("redstone_torch", Material.getMaterial("REDSTONE_TORCH_ON"));
            alternative.put("daylight_detector_1", Material.getMaterial("DAYLIGHT_DETECTOR"));
            alternative.put("daylight_detector_2", Material.getMaterial("DAYLIGHT_DETECTOR_INVERTED"));
            alternative.put("crafting_table", Material.getMaterial("WORKBENCH"));
            alternative.put("furnace", Material.getMaterial("BURNING_FURNACE"));
            alternative.put("snowball", Material.getMaterial("SNOW_BALL"));
            alternative.put("fireball", Material.getMaterial("FIREBALL"));
            alternative.put("lead", Material.getMaterial("LEASH"));
            alternative.put("carrot_on_a_stick", Material.getMaterial("CARROT_STICK"));
        }

        for (Material m : Material.values()) {
            String s = m.toString();

            if (BlockUtils.endsWith(s, "_SWORD")) {
                baseMultiplier.put(m, specialMultiplier);
            } else {
                switch (s) {
                    case "DIAMOND_PICKAXE":
                    case "DIAMOND_AXE":
                    case "DIAMOND_HOE":
                    case "DIAMOND_SHOVEL":
                    case "DIAMOND_SPADE":
                        baseMultiplier.put(m, 8.0);
                        break;
                    case "NETHERITE_PICKAXE":
                    case "NETHERITE_AXE":
                    case "NETHERITE_HOE":
                    case "NETHERITE_SHOVEL":
                        baseMultiplier.put(m, 9.0);
                        break;
                    case "STONE_PICKAXE":
                    case "STONE_AXE":
                    case "STONE_HOE":
                    case "STONE_SHOVEL":
                    case "STONE_SPADE":
                        baseMultiplier.put(m, 4.0);
                        break;
                    case "IRON_PICKAXE":
                    case "IRON_AXE":
                    case "IRON_HOE":
                    case "IRON_SHOVEL":
                    case "IRON_SPADE":
                        baseMultiplier.put(m, 6.0);
                        break;
                    case "WOODEN_PICKAXE":
                    case "WOOD_PICKAXE":
                    case "WOODEN_AXE":
                    case "WOOD_AXE":
                    case "WOODEN_HOE":
                    case "WOOD_HOE":
                    case "WOODEN_SHOVEL":
                    case "WOOD_SHOVEL":
                    case "WOOD_SPADE":
                        baseMultiplier.put(m, 2.0);
                        break;
                    case "GOLDEN_PICKAXE":
                    case "GOLD_PICKAXE":
                    case "GOLDEN_AXE":
                    case "GOLD_AXE":
                    case "GOLDEN_HOE":
                    case "GOLD_HOE":
                    case "GOLDEN_SHOVEL":
                    case "GOLD_SHOVEL":
                    case "GOLD_SPADE":
                        baseMultiplier.put(m, 12.0);
                        break;
                    case "SHEARS":
                        baseMultiplier.put(m, specialMultiplier);
                    default:
                        break;
                }
            }
        }
    }

    public static Material get(String s) {
        return alternative.get(s.toLowerCase());
    }

    public static Material correct(String recent, String older) {
        return Material.getMaterial(MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) ? recent.toUpperCase() : older.toUpperCase());
    }

    public static long getBlockBreakTime(SpartanPlayer player, ItemStack itemStack, Material blockType) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && blockType.isBlock()) {
            double multiplier = baseMultiplier.getOrDefault(itemStack.getType(), 1.0);
            boolean canHarvest = true; // No need for method, players cannot break such blocks, thus they will never be checked

            // Separator (Custom)
            if (multiplier == specialMultiplier) {
                if (itemStack.getType() == Material.SHEARS) {
                    if (BlockUtils.areWebs(blockType) || BlockUtils.areLeaves(blockType)) {
                        multiplier = 15.0;
                    } else if (BlockUtils.areWools(blockType)) {
                        multiplier = 5.0;
                    }
                } else if (BlockUtils.areWebs(blockType)) {
                    multiplier = 15.0;
                }
            }

            // Separator
            if (true) { // Ideal Tool (Method missing, no serious problems arise) [Example: Pickaxe for stone blocks]
                if (!canHarvest) {
                    multiplier = 1.0;
                } else {
                    int enchantmentLevel = itemStack.getEnchantmentLevel(Enchantment.DIG_SPEED);

                    if (enchantmentLevel > 0) {
                        multiplier += Math.pow(enchantmentLevel, 2) + 1.0;
                    }
                }
            }

            // Separator
            int hasteEffect = PlayerData.getPotionLevel(player, PotionEffectType.FAST_DIGGING);

            if (hasteEffect > 0) {
                multiplier *= (0.2 * hasteEffect) + 1.0;
            }

            // Separator
            int miningFatigueEffect = PlayerData.getPotionLevel(player, PotionEffectType.SLOW_DIGGING);

            if (miningFatigueEffect > 0) {
                multiplier *= Math.pow(miningFatigueEffect, 0.3);
            }

            // Separator
            boolean water;

            if (player.isSwimming()) {
                water = true;
            } else {
                SpartanBlock block = player.getLocation().clone().add(0, player.getEyeHeight(), 0).getBlock();
                water = block.isWaterLogged() || block.getType() == MaterialUtils.get("water");
            }
            if (water) {
                boolean aquaInfinity = false;

                if (itemStack.getEnchantmentLevel(Enchantment.WATER_WORKER) > 0) {
                    aquaInfinity = true;
                } else {
                    SpartanInventory inventory = player.getInventory();
                    List<ItemStack> items = new ArrayList<>(4 + 1);
                    items.addAll(Arrays.asList(inventory.getArmorContents()));
                    items.add(inventory.getItemInOffHand());

                    for (ItemStack item : items) {
                        if (item != null && item.getEnchantmentLevel(Enchantment.WATER_WORKER) > 0) {
                            aquaInfinity = true;
                            break;
                        }
                    }
                }

                if (!aquaInfinity) {
                    multiplier /= 5.0;
                }
            }
            if (!player.isOnGround()) {
                multiplier /= 5.0;
            }

            // Separator
            double damage = multiplier / blockType.getHardness();

            // Separator
            if (canHarvest) {
                damage /= 30.0;
            } else {
                damage /= 100.0;
            }

            // Separator
            if (damage > 1.0) {
                return 0L;
            }

            // Separator
            double ticks = Math.ceil(1.0 / damage);
            return AlgebraUtils.integerRound(ticks * 50L);
        }
        return -1L;
    }
}
