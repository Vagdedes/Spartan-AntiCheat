package com.vagdedes.spartan.utils.minecraft.world;

import com.vagdedes.spartan.abstraction.player.SpartanPlayer;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.inventory.MaterialUtils;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class BlockUtils {

    public static final long sensitiveBlockBreakTime = 250L;
    public static final Material
            head = MaterialUtils.get("head"),
            iron_bars = MaterialUtils.get("iron_bars"),
            cake = MaterialUtils.get("cake"),
            magma = MaterialUtils.get("magma"),
            beetroot_block = MaterialUtils.get("beetroot_block");

    public static final Set<Material> air, solid, sensitive, editable, chest, plate, ice, blue_ice, glass, glass_pane, slabs,
            climbable, climbableOriginal, door, entity_blocks, trap_door, liquid, banner, carpet, bed, shulker_box, stairs,
            fence, fence_gate, heads, leaves, egg, coral_fan, pot, anvil, cobble_walls, terracotta, concrete, candle,
            candleCake, dripleaf, ores, wood, wool, wire, semi_solid, changeable, walls, interactive_bushes, scaffolding,
            interactive_snow, interactive_and_passable, honey_block, slime_block, web, piston;

    public static boolean endsWith(String s, String ending) {
        return s.endsWith(ending) && !s.contains("LEGACY_");
    }

    private static boolean startsWith(String s, String start) {
        return s.startsWith(start) && !s.contains("LEGACY_");
    }

    private static boolean contains(String s, String start) {
        return s.contains(start) && !s.contains("LEGACY_");
    }

    // Separator

    static {
        Set<Material> builder = new HashSet<>(),
                helper = new HashSet<>();
        Material[] materials = Material.values();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.BUBBLE_COLUMN);
            builder.add(Material.KELP);
            builder.add(Material.KELP_PLANT);
            builder.add(Material.SEAGRASS);
            builder.add(Material.TALL_SEAGRASS);
            builder.add(Material.SEA_PICKLE);
            builder.add(Material.WATER);
            builder.add(Material.LAVA);
        } else {
            builder.add(Material.getMaterial("STATIONARY_WATER"));
            builder.add(Material.getMaterial("STATIONARY_LAVA"));
        }
        liquid = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                    builder.add(Material.GLOW_ITEM_FRAME);
                }
                builder.add(Material.SMITHING_TABLE);
                builder.add(Material.GRINDSTONE);
                builder.add(Material.FLETCHING_TABLE);
                builder.add(Material.STONECUTTER);
                builder.add(Material.CARTOGRAPHY_TABLE);
                builder.add(Material.BLAST_FURNACE);
                builder.add(Material.SMOKER);
                builder.add(Material.LOOM);
                builder.add(Material.BARREL);
                builder.add(Material.BELL);
            }
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BUTTON")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("WOOD_BUTTON"));
            builder.add(Material.STONE_BUTTON);
        }
        builder.add(Material.ITEM_FRAME);
        builder.add(Material.HOPPER);
        builder.add(Material.JUKEBOX);
        builder.add(Material.NOTE_BLOCK);
        builder.add(Material.DROPPER);
        builder.add(Material.BREWING_STAND);
        builder.add(Material.LEVER);
        builder.add(Material.ANVIL);
        builder.add(MaterialUtils.get("crafting_table"));
        builder.add(MaterialUtils.get("repeater_on"));
        builder.add(MaterialUtils.get("repeater_off"));
        builder.add(MaterialUtils.get("comparator_on"));
        builder.add(MaterialUtils.get("comparator_off"));
        builder.add(MaterialUtils.get("enchanting_table"));
        builder.add(MaterialUtils.get("end_portal_frame"));
        builder.add(MaterialUtils.get("furnace"));
        editable = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.CHEST);
        builder.add(Material.TRAPPED_CHEST);
        builder.add(Material.ENDER_CHEST);
        chest = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (s.contains("_PLATE")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("GOLD_PLATE"));
            builder.add(Material.getMaterial("IRON_PLATE"));
            builder.add(Material.getMaterial("STONE_PLATE"));
            builder.add(Material.getMaterial("WOOD_PLATE"));
        }
        plate = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(MaterialUtils.get("web"));
        web = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (contains(s, "WIRE")) {
                builder.add(m);
            }
        }
        wire = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_ORE")) {
                builder.add(m);
            }
        }
        ores = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.ICE);
        builder.add(Material.PACKED_ICE);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            builder.add(Material.FROSTED_ICE);
        }
        ice = new HashSet<>(builder);

        // Separator

        builder.clear();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)
                && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.BLUE_ICE);
        }
        blue_ice = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.GLASS_PANE);

            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_STAINED_GLASS_PANE")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("THIN_GLASS"));
            builder.add(Material.getMaterial("STAINED_GLASS_PANE"));
        }
        glass_pane = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
                builder.add(Material.BAMBOO_BLOCK);
                builder.add(Material.STRIPPED_BAMBOO_BLOCK);
                builder.add(Material.BAMBOO_MOSAIC);
                builder.add(Material.BAMBOO_PLANKS);
            }
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_LOG") || endsWith(s, "_WOOD")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("LOG"));
            builder.add(Material.getMaterial("LOG_2"));
        }
        wood = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.AIR);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_AIR")) {
                    builder.add(m);
                }
            }
        }
        air = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) { //
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_STAINED_GLASS")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("STAINED_GLASS"));
        }
        builder.add(Material.GLASS);
        glass = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.getMaterial("STEP"));
            builder.add(Material.getMaterial("WOOD_STEP"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (contains(s, "_SLAB")) {
                builder.add(m);
            }
        }
        slabs = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.LADDER);
        builder.add(Material.VINE);

        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_VINES") || endsWith(s, "_VINES_PLANT")) {
                builder.add(m);
            }
        }
        climbableOriginal = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.addAll(climbableOriginal);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                builder.add(Material.POWDER_SNOW);
            }
            builder.add(Material.SCAFFOLDING);
        }
        climbable = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.SAND);
        builder.add(Material.GRAVEL);

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                for (Material m : materials) {
                    String s = m.toString();

                    if (endsWith(s, "_CONCRETE_POWDER")) {
                        builder.add(m);
                    }
                }
            } else {
                builder.add(Material.getMaterial("CONCRETE_POWDER"));
            }
        }
        entity_blocks = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.TERRACOTTA);

            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_TERRACOTTA")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("STAINED_CLAY"));
        }
        terracotta = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_WOOL")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("WOOL"));
        }
        wool = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_CONCRETE")) {
                    builder.add(m);
                }
            }
        }
        concrete = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
                        builder.add(Material.BAMBOO_DOOR);
                    }
                    builder.add(Material.MANGROVE_DOOR);
                }
                builder.add(Material.WARPED_DOOR);
                builder.add(Material.CRIMSON_DOOR);
            }
            builder.add(Material.IRON_DOOR);
            builder.add(Material.OAK_DOOR);
        } else {
            builder.add(Material.getMaterial("IRON_DOOR_BLOCK"));
            builder.add(Material.getMaterial("WOODEN_DOOR"));
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            builder.add(Material.ACACIA_DOOR);
            builder.add(Material.BIRCH_DOOR);
            builder.add(Material.DARK_OAK_DOOR);
            builder.add(Material.JUNGLE_DOOR);
            builder.add(Material.SPRUCE_DOOR);
        }
        door = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_TRAPDOOR")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("TRAP_DOOR"));

            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
                builder.add(Material.IRON_TRAPDOOR);
            }
        }
        trap_door = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_CARPET")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("CARPET"));
        }
        carpet = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BED")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("BED_BLOCK"));
        }
        bed = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BANNER")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("STANDING_BANNER"));
            builder.add(Material.getMaterial("WALL_BANNER"));
        }
        banner = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_11)) {
            for (Material m : materials) {
                String s = m.toString();

                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
                    builder.add(Material.SHULKER_BOX);
                }
                if (endsWith(s, "_SHULKER_BOX")) {
                    builder.add(m);
                }
            }
        }
        shulker_box = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_STAIRS")) {
                builder.add(m);
            }
        }
        stairs = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.getMaterial("FENCE"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_FENCE")) {
                builder.add(m);
            }
        }
        fence = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "CANDLE")) {
                    builder.add(m);
                }
            }
        }
        candle = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "CANDLE_CAKE")) {
                    builder.add(m);
                }
            }
        }
        candleCake = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            for (Material m : materials) {
                String s = m.toString();

                if (contains(s, "DRIPLEAF")) {
                    builder.add(m);
                }
            }
        }
        dripleaf = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.getMaterial("FENCE_GATE"));
        }
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_FENCE_GATE")) {
                builder.add(m);
            }
        }
        fence_gate = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.SKELETON_SKULL);
            builder.add(Material.SKELETON_WALL_SKULL);
            builder.add(Material.WITHER_SKELETON_SKULL);
            builder.add(Material.WITHER_SKELETON_WALL_SKULL);
            builder.add(Material.CREEPER_HEAD);
            builder.add(Material.CREEPER_WALL_HEAD);
            builder.add(Material.DRAGON_HEAD);
            builder.add(Material.DRAGON_WALL_HEAD);
            builder.add(Material.PLAYER_HEAD);
            builder.add(Material.PLAYER_WALL_HEAD);
            builder.add(Material.ZOMBIE_HEAD);
            builder.add(Material.ZOMBIE_WALL_HEAD);
        } else {
            builder.add(Material.getMaterial("SKULL"));
        }
        heads = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_LEAVES")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("LEAVES"));
            builder.add(Material.getMaterial("LEAVES_2"));
        }
        leaves = new HashSet<>(builder);

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_SPAWN_EGG")) {
                    builder.add(m);
                }
            }
        } else {
            builder.add(Material.getMaterial("MONSTER_EGG"));
        }
        egg = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.BRAIN_CORAL_FAN);
            builder.add(Material.BRAIN_CORAL);
            builder.add(Material.DEAD_BRAIN_CORAL_FAN);
            builder.add(Material.DEAD_BRAIN_CORAL);
            builder.add(Material.DEAD_BRAIN_CORAL_WALL_FAN);

            builder.add(Material.BUBBLE_CORAL_FAN);
            builder.add(Material.BUBBLE_CORAL);
            builder.add(Material.DEAD_BUBBLE_CORAL_FAN);
            builder.add(Material.DEAD_BUBBLE_CORAL);
            builder.add(Material.DEAD_BUBBLE_CORAL_WALL_FAN);

            builder.add(Material.FIRE_CORAL_FAN);
            builder.add(Material.FIRE_CORAL);
            builder.add(Material.DEAD_FIRE_CORAL_FAN);
            builder.add(Material.DEAD_FIRE_CORAL);
            builder.add(Material.DEAD_FIRE_CORAL_WALL_FAN);

            builder.add(Material.HORN_CORAL_FAN);
            builder.add(Material.HORN_CORAL);
            builder.add(Material.DEAD_HORN_CORAL_FAN);
            builder.add(Material.DEAD_HORN_CORAL);
            builder.add(Material.DEAD_HORN_CORAL_WALL_FAN);

            builder.add(Material.TUBE_CORAL_FAN);
            builder.add(Material.TUBE_CORAL);
            builder.add(Material.DEAD_TUBE_CORAL_FAN);
            builder.add(Material.DEAD_TUBE_CORAL);
            builder.add(Material.DEAD_TUBE_CORAL_WALL_FAN);
        }
        coral_fan = new HashSet<>(builder);

        // Separator

        builder.clear();
        builder.add(Material.FLOWER_POT);
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            for (Material m : materials) {
                String s = m.toString();

                if (startsWith(s, "POTTED_")) {
                    builder.add(m);
                }
            }
        }
        pot = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.ANVIL);
            builder.add(Material.CHIPPED_ANVIL);
            builder.add(Material.DAMAGED_ANVIL);
        } else {
            builder.add(Material.ANVIL);
        }
        anvil = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_WALL")) {
                builder.add(m);
            }
        }
        cobble_walls = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
            builder.add(Material.POWDER_SNOW);
        }
        interactive_snow = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            builder.add(Material.SCAFFOLDING);
        }
        scaffolding = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            if (areDripLeafs(m)
                    || areWebs(m)
                    || isPowderSnow(m)
                    || isScaffoldingBlock(m)) {
                builder.add(m);
            }
        }
        interactive_and_passable = new HashSet<>(builder);

        // Separator

        builder.clear();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            builder.add(Material.PISTON);
            builder.add(Material.STICKY_PISTON);
        } else {
            builder.add(Material.getMaterial("PISTON_BASE"));
            builder.add(Material.getMaterial("PISTON_STICKY_BASE"));
        }
        piston = new HashSet<>(builder);

        // Separator

        builder.clear();
        helper.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                            helper.add(Material.SCULK_SENSOR);
                            helper.add(Material.SCULK_SHRIEKER);
                            helper.add(Material.MUD);
                        }
                        helper.add(Material.DIRT_PATH);
                        helper.add(Material.LIGHTNING_ROD);
                        helper.add(Material.POINTED_DRIPSTONE);
                        helper.add(Material.SMALL_AMETHYST_BUD);
                        helper.add(Material.MEDIUM_AMETHYST_BUD);
                        helper.add(Material.LARGE_AMETHYST_BUD);
                        helper.add(Material.AMETHYST_CLUSTER);
                        helper.add(Material.POWDER_SNOW_CAULDRON);
                        helper.add(Material.LAVA_CAULDRON);
                        helper.add(Material.WATER_CAULDRON);
                        helper.add(Material.CAVE_VINES);
                        helper.add(Material.CAVE_VINES_PLANT);
                    }
                    helper.add(Material.SOUL_CAMPFIRE);
                    helper.add(Material.TWISTING_VINES);
                    helper.add(Material.TWISTING_VINES_PLANT);
                    helper.add(Material.WEEPING_VINES);
                    helper.add(Material.WEEPING_VINES_PLANT);
                    helper.add(Material.CHAIN);
                    helper.add(Material.SOUL_LANTERN);
                }
                helper.add(Material.BELL);
                helper.add(Material.LANTERN);
                helper.add(Material.CAMPFIRE);
                helper.add(Material.COMPOSTER);
                helper.add(Material.LECTERN);
                helper.add(Material.GRINDSTONE);
                helper.add(Material.STONECUTTER);
                helper.add(Material.BAMBOO);
            }
            helper.add(Material.PISTON_HEAD);
            helper.add(Material.COBBLESTONE_WALL);
            helper.add(Material.MOSSY_COBBLESTONE_WALL);
            helper.add(Material.IRON_BARS);
            helper.add(Material.PISTON);
            helper.add(Material.STICKY_PISTON);
            helper.add(Material.FARMLAND);
            helper.add(Material.TURTLE_EGG);
            helper.add(Material.CONDUIT);
        } else {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                helper.add(Material.CHORUS_PLANT);
                helper.add(Material.END_CRYSTAL);
                helper.add(Material.END_ROD);
                helper.add(Material.getMaterial("GRASS_PATH"));
                helper.add(beetroot_block);
            }
            helper.add(Material.getMaterial("PISTON_EXTENSION"));
            helper.add(Material.getMaterial("COBBLE_WALL"));
            helper.add(Material.getMaterial("IRON_FENCE"));
            helper.add(Material.getMaterial("SOIL"));
            helper.add(Material.getMaterial("PISTON_BASE"));
            helper.add(Material.getMaterial("PISTON_STICKY_BASE"));
        }

        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_HANGING_SIGN")) {
                helper.add(m);
            }
        }
        helper.add(MaterialUtils.get("web"));
        helper.add(Material.COCOA);
        helper.add(Material.DRAGON_EGG);
        helper.add(Material.HOPPER);
        helper.add(Material.FLOWER_POT);
        helper.add(Material.BREWING_STAND);
        helper.add(Material.CAULDRON);
        helper.add(Material.LADDER);
        helper.add(Material.VINE);
        helper.add(Material.SNOW);
        helper.add(Material.SNOW_BLOCK);
        helper.add(Material.CACTUS);
        helper.add(Material.SOUL_SAND);
        helper.add(MaterialUtils.get("daylight_detector_1"));
        helper.add(MaterialUtils.get("daylight_detector_2"));
        helper.add(MaterialUtils.get("repeater_on"));
        helper.add(MaterialUtils.get("repeater_off"));
        helper.add(MaterialUtils.get("comparator_on"));
        helper.add(MaterialUtils.get("comparator_off"));
        helper.add(MaterialUtils.get("enchanting_table"));
        helper.add(MaterialUtils.get("lily_pad"));
        helper.add(MaterialUtils.get("end_portal_frame"));
        helper.add(MaterialUtils.get("cake"));

        for (Material m : materials) {
            if (areFenceGates(m)
                    || areFences(m)
                    || areStairs(m)
                    || areSlabs(m)
                    || areTrapdoors(m)
                    || areDoors(m)
                    || areStainedGlasses(m)
                    || areChests(m)
                    || arePlates(m)
                    || areShulkerBoxes(m)
                    || areLeaves(m)
                    || areHeads(m)
                    || areCoralFans(m)
                    || areAnvils(m)
                    || areCarpets(m)
                    || areBeds(m)
                    || areFlowerPots(m)
                    || areCobbleWalls(m)
                    || areCandles(m)
                    || areCandleCakes(m)
                    || isInteractiveAndPassable(null, m)
                    || helper.contains(m)) {
                builder.add(m);
            }
        }
        semi_solid = new HashSet<>(builder);

        // Separator

        builder.clear();
        for (Material m : materials) {
            if (editable.contains(m)
                    || areFenceGates(m)
                    || areTrapdoors(m)
                    || areChests(m)
                    || areDoors(m)
                    || areShulkerBoxes(m)
                    || areBeds(m)
                    || areAnvils(m)) {
                builder.add(m);
            }
        }
        changeable = new HashSet<>(builder);

        // Separator

        builder.clear();
        helper.clear();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            helper.add(Material.SCAFFOLDING);
            helper.add(Material.WITHER_ROSE);
            helper.add(Material.SWEET_BERRY_BUSH);
            helper.add(Material.CORNFLOWER);
            helper.add(Material.LILY_OF_THE_VALLEY);
        } else {
            helper.add(Material.getMaterial("SIGN"));
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_20)) {
                            helper.add(Material.PINK_PETALS);
                        }
                        helper.add(Material.SCULK_VEIN);
                    }
                    helper.add(Material.GLOW_LICHEN);
                    helper.add(Material.SPORE_BLOSSOM);
                    helper.add(Material.HANGING_ROOTS);
                    helper.add(Material.CAVE_VINES);
                    helper.add(Material.CAVE_VINES_PLANT);
                    helper.add(Material.POWDER_SNOW);
                    helper.add(Material.LIGHT);
                }
                helper.add(Material.SOUL_TORCH);
                helper.add(Material.SOUL_WALL_TORCH);
                helper.add(Material.SOUL_FIRE);
                helper.add(Material.TWISTING_VINES);
                helper.add(Material.TWISTING_VINES_PLANT);
                helper.add(Material.WEEPING_VINES);
                helper.add(Material.WEEPING_VINES_PLANT);
                helper.add(Material.WARPED_ROOTS);
                helper.add(Material.CRIMSON_ROOTS);
                helper.add(Material.WARPED_FUNGUS);
                helper.add(Material.CRIMSON_FUNGUS);
                helper.add(Material.NETHER_SPROUTS);
            }
            Material material = MaterialUtils.findMaterial("SHORT_GRASS");

            if (material != null) {
                helper.add(material);
            }
            helper.add(Material.REDSTONE_TORCH);
            helper.add(Material.REDSTONE_WALL_TORCH);
            helper.add(Material.WALL_TORCH);
            helper.add(Material.RAIL);
            helper.add(Material.END_PORTAL);
            helper.add(Material.NETHER_PORTAL);
            helper.add(Material.MOVING_PISTON);
            helper.add(Material.DANDELION);
            helper.add(Material.POPPY);
            helper.add(Material.TALL_GRASS);
            helper.add(Material.POTATOES);
            helper.add(Material.CARROTS);
            helper.add(Material.NETHER_WART);
            helper.add(Material.ACTIVATOR_RAIL);
            helper.add(Material.DETECTOR_RAIL);
            helper.add(Material.POWERED_RAIL);
            helper.add(Material.BEETROOT_SEEDS);
            helper.add(Material.MELON_SEEDS);
            helper.add(Material.PUMPKIN_SEEDS);
            helper.add(Material.WHEAT);
            helper.add(Material.WHEAT_SEEDS);
            helper.add(Material.BUBBLE_COLUMN);
            helper.add(Material.CAVE_AIR);
            helper.add(Material.VOID_AIR);
            helper.add(Material.KELP);
            helper.add(Material.KELP_PLANT);
            helper.add(Material.BEETROOTS);
            helper.add(Material.FERN);
            helper.add(Material.LARGE_FERN);
            helper.add(Material.SUNFLOWER);
            helper.add(Material.AZURE_BLUET);
            helper.add(Material.ATTACHED_MELON_STEM);
            helper.add(Material.ATTACHED_PUMPKIN_STEM);
            helper.add(Material.ROSE_BUSH);
            helper.add(Material.ALLIUM);
            helper.add(Material.OXEYE_DAISY);
            helper.add(Material.BLUE_ORCHID);
            helper.add(Material.LILAC);
            helper.add(Material.PEONY);

            for (Material m : materials) {
                String s = m.toString();

                if (endsWith(s, "_BUTTON")

                        || !startsWith(s, "POTTED_")
                        && (endsWith(s, "_SAPLING")
                        || endsWith(s, "_TULIP"))) {
                    helper.add(m);
                }
            }
        } else {
            helper.add(Material.getMaterial("REDSTONE_TORCH_ON"));
            helper.add(Material.getMaterial("REDSTONE_TORCH_OFF"));
            helper.add(Material.getMaterial("SEEDS"));
            helper.add(Material.getMaterial("RAILS"));
            helper.add(Material.getMaterial("ENDER_PORTAL"));
            helper.add(Material.getMaterial("PORTAL"));
            helper.add(Material.getMaterial("PISTON_MOVING_PIECE"));
            helper.add(Material.getMaterial("CROPS"));
            helper.add(Material.getMaterial("SIGN_POST"));
            helper.add(Material.getMaterial("RED_ROSE"));
            helper.add(Material.getMaterial("FLOWER"));
            helper.add(Material.getMaterial("YELLOW_FLOWER"));
            helper.add(Material.getMaterial("SUGAR_CANE_BLOCK"));
            helper.add(Material.getMaterial("SAPLING"));
            helper.add(Material.getMaterial("LONG_GRASS"));
            helper.add(Material.getMaterial("NETHER_WARTS"));
            helper.add(Material.getMaterial("DOUBLE_PLANT"));
            helper.add(Material.getMaterial("NETHER_WARTS"));
            helper.add(Material.getMaterial("WOOD_BUTTON"));
            helper.add(Material.getMaterial("GRASS"));
            helper.add(Material.STONE_BUTTON);
        }
        for (Material m : materials) {
            String s = m.toString();

            if (endsWith(s, "_SIGN") && !contains(s, "_HANGING_")
                    || endsWith(s, "_CROP")) {
                helper.add(m);
            }
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10)) {
                    helper.add(Material.STRUCTURE_VOID);
                }
                helper.add(Material.BEETROOT_SEEDS);
                helper.add(Material.END_GATEWAY);
            }
            helper.add(Material.ARMOR_STAND);
        }
        helper.add(Material.SUGAR_CANE);
        helper.add(Material.AIR);
        helper.add(Material.BROWN_MUSHROOM);
        helper.add(Material.RED_MUSHROOM);
        helper.add(Material.TORCH);
        helper.add(Material.TRIPWIRE);
        helper.add(Material.TRIPWIRE_HOOK);
        helper.add(Material.REDSTONE_WIRE);
        helper.add(Material.ACTIVATOR_RAIL);
        helper.add(Material.DETECTOR_RAIL);
        helper.add(Material.POWERED_RAIL);
        helper.add(Material.MELON_SEEDS);
        helper.add(Material.PUMPKIN_SEEDS);
        helper.add(Material.PUMPKIN_STEM);
        helper.add(Material.MELON_STEM);
        helper.add(Material.CARROT);
        helper.add(Material.FIRE);
        helper.add(Material.POTATO);
        helper.add(Material.LEVER);
        helper.add(Material.DEAD_BUSH);
        helper.add(Material.VINE);

        for (Material m : materials) {
            if (!areFenceGates(m)
                    && !areTrapdoors(m)
                    && !arePlates(m)
                    && !isLiquid(m)
                    && !areDripLeafs(m)
                    && !areBanners(m)
                    && !helper.contains(m)) {
                builder.add(m);
            }
        }
        solid = new HashSet<>(builder);

        // Separator

        builder.clear();
        helper.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            helper.add(Material.END_CRYSTAL);
            helper.add(Material.CHORUS_PLANT);
        }
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_19)) {
                                helper.add(Material.MUD);
                            }
                            helper.add(Material.GLOW_LICHEN);
                            helper.add(Material.GLOW_ITEM_FRAME);
                            helper.add(Material.AZALEA);
                            helper.add(Material.FLOWERING_AZALEA);
                            helper.add(Material.TINTED_GLASS);
                            helper.add(Material.CALCITE);
                        }
                        helper.add(Material.WARPED_WART_BLOCK);
                        helper.add(Material.NETHER_WART_BLOCK);
                        helper.add(Material.WARPED_NYLIUM);
                        helper.add(Material.CRIMSON_NYLIUM);
                    }
                    helper.add(Material.HONEY_BLOCK);
                }
                helper.add(Material.BAMBOO);
            }
            helper.add(Material.TURTLE_EGG);
            helper.add(Material.BROWN_MUSHROOM);
            helper.add(Material.RED_MUSHROOM);
            helper.add(Material.BROWN_MUSHROOM_BLOCK);
            helper.add(Material.RED_MUSHROOM_BLOCK);
            helper.add(Material.MUSHROOM_STEM);
            helper.add(Material.CUT_SANDSTONE);
            helper.add(Material.CUT_RED_SANDSTONE);
            helper.add(Material.PODZOL);

            for (Material m : materials) {
                String s = m.toString();

                if (startsWith(s, "INFESTED_")) {
                    helper.add(m);
                }
            }
        } else {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10)) {
                        helper.add(magma);
                    }
                    helper.add(Material.END_ROD);
                    helper.add(beetroot_block);
                }
                helper.add(Material.SLIME_BLOCK);
                helper.add(Material.RED_SANDSTONE);
                helper.add(Material.RED_SANDSTONE_STAIRS);
            }
            helper.add(Material.getMaterial("HUGE_MUSHROOM_1"));
            helper.add(Material.getMaterial("HUGE_MUSHROOM_2"));
        }
        helper.add(Material.ITEM_FRAME);
        helper.add(Material.LADDER);
        helper.add(Material.TNT);
        helper.add(Material.COCOA);
        helper.add(Material.NETHERRACK);
        helper.add(Material.GLASS);
        helper.add(Material.SANDSTONE);
        helper.add(Material.SANDSTONE_STAIRS);
        helper.add(Material.QUARTZ_BLOCK);
        helper.add(Material.QUARTZ_STAIRS);
        helper.add(Material.SNOW);
        helper.add(Material.SNOW_BLOCK);
        helper.add(Material.SOUL_SAND);
        helper.add(Material.PUMPKIN);
        helper.add(MaterialUtils.get("lily_pad"));
        helper.add(MaterialUtils.get("daylight_detector_1"));
        helper.add(MaterialUtils.get("daylight_detector_2"));
        helper.add(MaterialUtils.get("repeater_on"));
        helper.add(MaterialUtils.get("repeater_off"));
        helper.add(MaterialUtils.get("comparator_on"));
        helper.add(MaterialUtils.get("comparator_off"));
        helper.add(MaterialUtils.get("enchanting_table"));

        for (Material m : materials) {
            if (!isSolid(m)
                    || areGlasses(m)
                    || areStainedGlasses(m)
                    || areIceBlocks(m)
                    || areCoralFans(m)
                    || areLeaves(m)
                    || areHeads(m)
                    || areBeds(m)
                    || areCarpets(m)
                    || areFlowerPots(m)
                    || isLiquid(m)
                    || isScaffoldingBlock(m)
                    || isPowderSnow(m)
                    || areDripLeafs(m)
                    || areCandles(m)
                    || areCandleCakes(m)
                    || helper.contains(m)) {
                builder.add(m);
            }
        }
        sensitive = new HashSet<>(builder);

        // Separator

        builder.clear();
        helper.clear();

        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
            if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_16)) {
                    if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17)) {
                        helper.add(Material.LIGHTNING_ROD);
                    }
                    helper.add(Material.CHAIN);
                }
                helper.add(Material.HONEY_BLOCK);
            }
            helper.add(Material.CHORUS_PLANT);
            helper.add(Material.END_ROD);
        }
        helper.add(Material.CACTUS);
        helper.add(Material.COCOA);
        helper.add(Material.DRAGON_EGG);
        helper.add(iron_bars);
        helper.add(cake);
        helper.add(head);

        for (Material m : materials) {
            if (areStainedGlasses(m)
                    || areDoors(m)
                    || areTrapdoors(m)
                    || areChests(m)
                    || areFences(m)
                    || areFenceGates(m)
                    || areCobbleWalls(m)
                    || areFlowerPots(m)
                    || areAnvils(m)
                    || helper.contains(m)) {
                builder.add(m);
            }
        }
        walls = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)) {
            builder.add(Material.SWEET_BERRY_BUSH);
        }

        interactive_bushes = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_15)) {
            builder.add(Material.HONEY_BLOCK);
        }

        honey_block = new HashSet<>(builder);

        // Separator

        builder.clear();
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            builder.add(Material.SLIME_BLOCK);
        }

        slime_block = new HashSet<>(builder);

        // Separator

        builder.clear();
        helper.clear();
    }

    public static int getMaxHeight(World world) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMaxHeight() : 256;
    }

    public static int getMinHeight(World world) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_17) ? world.getMinHeight() : 0;
    }

    public static boolean areCandles(Material m) {
        return candle.contains(m);
    }

    public static boolean areCandleCakes(Material m) {
        return candleCake.contains(m);
    }

    public static boolean isScaffoldingBlock(Material m) {
        return scaffolding.contains(m);
    }

    public static boolean areWalls(Material m) {
        return walls.contains(m);
    }

    public static boolean areHeads(Material m) {
        return heads.contains(m);
    }

    public static boolean areFlowerPots(Material m) {
        return pot.contains(m);
    }

    public static boolean isLiquid(Block block) {
        return isLiquid(block.getType()) || block.isLiquid();
    }

    public static boolean isWaterLogged(BlockData blockData) {
        return MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                && blockData instanceof Waterlogged && ((Waterlogged) blockData).isWaterlogged();
    }

    public static boolean isLiquid(Material m) {
        return liquid.contains(m);
    }

    public static boolean areStairs(Material m) {
        return stairs.contains(m);
    }

    public static boolean areCobbleWalls(Material m) {
        return cobble_walls.contains(m);
    }

    public static boolean areSlabs(Material m) {
        return slabs.contains(m);
    }

    public static boolean areLeaves(Material m) {
        return leaves.contains(m);
    }

    public static boolean areCarpets(Material m) {
        return carpet.contains(m);
    }

    public static boolean areBeds(Material m) {
        return bed.contains(m);
    }

    public static boolean areEggs(Material m) {
        return egg.contains(m);
    }

    public static boolean areTrapdoors(Material m) {
        return trap_door.contains(m);
    }

    public static boolean areFences(Material m) {
        return fence.contains(m);
    }

    public static boolean areFenceGates(Material m) {
        return fence_gate.contains(m);
    }

    public static boolean isPowderSnow(Material m) {
        return interactive_snow.contains(m);
    }

    public static boolean areDripLeafs(Material m) {
        return dripleaf.contains(m);
    }

    public static boolean areAnvils(Material m) {
        return anvil.contains(m);
    }

    public static boolean areWires(Material m) {
        return wire.contains(m);
    }

    public static boolean areChests(Material m) {
        return chest.contains(m);
    }

    public static boolean areDoors(Material m) {
        return door.contains(m);
    }

    public static boolean areGlasses(Material m) {
        return glass.contains(m);
    }

    public static boolean areStainedGlasses(Material m) {
        return glass_pane.contains(m);
    }

    public static boolean arePlates(Material m) {
        return plate.contains(m);
    }

    public static boolean areIceBlocks(Material m) {
        return ice.contains(m) || blue_ice.contains(m);
    }

    public static boolean areRegularIceBlocks(Material m) {
        return ice.contains(m);
    }

    public static boolean areBlueIceBlocks(Material m) {
        return blue_ice.contains(m);
    }

    public static boolean areWools(Material m) {
        return wool.contains(m);
    }

    public static boolean areEntityBlocks(Material m) {
        return entity_blocks.contains(m) || areAnvils(m);
    }

    public static boolean areWebs(Material m) {
        return web.contains(m);
    }

    public static boolean arePistons(Material m) {
        return piston.contains(m);
    }

    public static boolean areShulkerBoxes(Material m) {
        return shulker_box.contains(m);
    }

    public static boolean areBanners(Material m) {
        return banner.contains(m);
    }

    public static boolean areCoralFans(Material m) {
        return coral_fan.contains(m);
    }

    public static boolean areTerracotta(Material m) {
        return terracotta.contains(m);
    }

    public static boolean areOres(Material m) {
        return ores.contains(m);
    }

    public static boolean areAir(Material m) {
        return air.contains(m);
    }

    public static boolean areWoods(Material m) {
        return wood.contains(m);
    }

    public static boolean isChangeable(Material m) {
        return changeable.contains(m);
    }

    public static boolean areConcrete(Material m) {
        return concrete.contains(m);
    }

    public static boolean areHoneyBlocks(Material m) {
        return honey_block.contains(m);
    }

    public static boolean areSlimeBlocks(Material m) {
        return slime_block.contains(m);
    }

    public static boolean areBouncingBlocks(Material material) {
        return honey_block.contains(material)
                || slime_block.contains(material)
                || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12) && bed.contains(material);
    }

    public static boolean areInteractiveBushes(Material m) {
        return interactive_bushes.contains(m);
    }

    public static boolean canClimb(Material m, boolean original) {
        return original ? climbableOriginal.contains(m) : climbable.contains(m);
    }

    public static boolean isSensitive(Material m) {
        return sensitive.contains(m);
    }

    private static boolean isSensitive(Material m, long time) {
        return time >= 0L && time <= sensitiveBlockBreakTime || sensitive.contains(m);
    }

    public static boolean isSensitive(SpartanPlayer p, Material m) {
        return isSensitive(
                m,
                p == null ? -1 : MaterialUtils.getBlockBreakTime(p, p.getItemInHand(), m)
        );
    }

    public static boolean isSolid(Material m) {
        return solid.contains(m) || semi_solid.contains(m);
    }

    public static boolean isFullSolid(Material m) {
        return solid.contains(m) && !semi_solid.contains(m);
    }

    public static boolean isSemiSolid(Material m) {
        return semi_solid.contains(m);
    }

    public static boolean isInteractiveAndPassable(SpartanPlayer p, Material material) {
        return interactive_and_passable.contains(material)
                || p != null && p.isFrozen();
    }

    // Separator

    public static boolean hasMaterial(ItemStack itemStack) {
        return itemStack != null && itemStack.getType() != Material.AIR;
    }

    public static String environmentToString(World.Environment e) {
        return toString(e);
    }

    public static String blockToString(Block b) {
        return toString(new SpartanBlock(b).material.toString());
    }

    public static String blockToString(SpartanBlock b) {
        return toString(b.material.toString());
    }

    public static String materialToString(Material m) {
        return toString(m);
    }

    private static String toString(Object obj) {
        return obj.toString().toLowerCase().replace("_", "-");
    }

    // Separator

    public static boolean isSlime(SpartanPlayer p, SpartanLocation loc, int blocks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_8)) {
            if (p.isOnGround(false)) {
                return loc.getBlock().material == Material.SLIME_BLOCK;
            } else {
                SpartanLocation loopLocation = loc.clone();

                if (!isSolid(loopLocation.getBlock().material)) {
                    int y = loc.getBlockY();

                    for (int i = y; i > Math.max(getMinHeight(p.getWorld()), y - blocks); i--) {
                        Material m = loopLocation.add(0, -1, 0).getBlock().material;

                        if (m == Material.SLIME_BLOCK) {
                            return true;
                        } else if (isSolid(m)
                                && m != Material.SNOW
                                && !areCarpets(m)
                                && !areSlabs(m)
                                && !areStairs(m)
                                && !areWalls(m)
                                && !canClimb(m, false)) {
                            break;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isBed(SpartanPlayer p, SpartanLocation loc, int blocks) {
        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_12)) {
            if (p.isOnGround(false)) {
                return areBeds(loc.getBlock().material);
            } else {
                SpartanLocation loopLocation = loc.clone();

                if (!isSolid(loopLocation.getBlock().material)) {
                    int y = loc.getBlockY();

                    for (int i = y; i > Math.max(getMinHeight(p.getWorld()), y - blocks); i--) {
                        Material m = loopLocation.add(0, -1, 0).getBlock().material;

                        if (areBeds(m)) {
                            return true;
                        } else if (isSolid(m)) {
                            break;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean isBouncingBlock(SpartanPlayer p, SpartanLocation loc, int blocks) {
        return isSlime(p, loc, blocks) || isBed(p, loc, blocks);
    }

}
