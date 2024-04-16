package com.vagdedes.spartan.utils.java;

import com.vagdedes.spartan.abstraction.replicates.SpartanBlock;
import com.vagdedes.spartan.abstraction.replicates.SpartanLocation;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayer;
import com.vagdedes.spartan.abstraction.replicates.SpartanPlayerDamage;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.gameplay.BlockUtils;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;
import com.vagdedes.spartan.utils.gameplay.PlayerUtils;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class HashUtils {

    public static final int
            trueHash = Boolean.hashCode(true),
            falseHash = Boolean.hashCode(false);
    private static final int
            bedWordHash = "BED".hashCode();

    public static int extendInt(int hash, int extra) {
        return (hash * SpartanBukkit.hashCodeMultiplier) + extra;
    }

    // Separator

    public static <E> int fastAbstractCollection(Collection<E> collection) {
        int size = collection.size();
        return size > 0 ? (size * SpartanBukkit.hashCodeMultiplier) + collection.iterator().next().hashCode() : 1;
    }

    public static int fastCollection(Collection<Number> collection) {
        Iterator<Number> iterator = collection.iterator();
        int hash = (iterator.next().hashCode() * SpartanBukkit.hashCodeMultiplier) + collection.size();
        Number value = null;

        while (iterator.hasNext()) {
            value = iterator.next();
        }
        if (value != null) {
            hash = extendInt(hash, value.hashCode());
        }
        return hash;
    }

    // Separator

    public static int collection(Collection<Float> collection) {
        int hash = 1;

        for (float value : collection) {
            hash = extendInt(hash, Float.hashCode(value));
        }
        return hash;
    }

    public static int collection(Collection<Number> collection, int fromIndex, int toIndex) {
        Iterator<Number> iterator = collection.iterator();

        if (fromIndex > 0) {
            for (int i = 0; i < fromIndex; i++) {
                if (iterator.hasNext()) {
                    iterator.next();
                } else {
                    return 1;
                }
            }

        }
        int hash = 1;
        int pos = fromIndex;

        while (iterator.hasNext()) {
            hash = extendInt(hash, iterator.next().hashCode());

            if (pos == toIndex) {
                return hash;
            }
            pos++;
        }
        return 1;
    }

    // Separator

    public static int hashPlayer(SpartanPlayer player) {
        int hash = Boolean.hashCode(player.bedrockPlayer);
        Entity vehicle = player.getVehicle();
        Collection<PotionEffect> potionEffects;

        if (vehicle != null) {
            hash = extendInt(hash, vehicle.getType().toString().hashCode());

            if (vehicle instanceof LivingEntity) {
                potionEffects = ((LivingEntity) vehicle).getActivePotionEffects();
            } else {
                potionEffects = new ArrayList<>(0);
            }
        } else {
            Set<Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage>> damages = player.getRawReceivedDamages();

            if (!damages.isEmpty()) {
                for (Map.Entry<EntityDamageEvent.DamageCause, SpartanPlayerDamage> entry : damages) {
                    long ticksPassed = entry.getValue().ticksPassed();

                    if (ticksPassed <= TPS.maximum) {
                        EntityDamageByEntityEvent event = entry.getValue().getEntityDamageByEntityEvent();

                        if (event != null) {
                            hash = extendInt(hash, event.getDamager().getType().toString().hashCode());
                            hash = extendInt(hash, entry.getValue().getActiveItem().getType().toString().hashCode());
                        }
                        hash = extendInt(hash, entry.getKey().toString().hashCode());
                        hash = extendInt(hash, Long.hashCode(ticksPassed));
                        hash = extendInt(hash,
                                Double.hashCode(
                                        AlgebraUtils.cut(entry.getValue().location.distance(player.movement.getLocation()),
                                                1
                                        )
                                )
                        );
                    } else {
                        hash = extendInt(hash, falseHash);
                    }
                }
            }
            hash = extendInt(hash, player.getGameMode().toString().hashCode());
            hash = extendInt(hash, Double.hashCode(player.getEyeHeight()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isGliding()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isSwimming()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isCrawling()));
            hash = extendInt(hash, Boolean.hashCode(player.isFrozen()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isSneaking()));
            hash = extendInt(hash, Boolean.hashCode(player.movement.isFlying()));
            hash = extendInt(hash, Boolean.hashCode(player.isOnGround()));
            hash = extendInt(hash, Math.min(AlgebraUtils.integerFloor(player.getPing() / TPS.tickTimeDecimal), 1000));
            potionEffects = player.getActivePotionEffects();
        }
        if (!potionEffects.isEmpty()) {
            Map<Integer, PotionEffect> map = new TreeMap<>();

            for (PotionEffect potionEffect : potionEffects) {
                map.put(potionEffect.getType().toString().hashCode(), potionEffect);
            }
            for (PotionEffect potionEffect : map.values()) {
                hash = extendInt(hash, potionEffect.getType().toString().hashCode());
                hash = extendInt(hash, potionEffect.getAmplifier());
            }
        }
        return hash;
    }

    public static int hashInventory(SpartanPlayer player) {
        int hash = Boolean.hashCode(player.isUsingItem());
        ItemStack[] armors = player.getInventory().getArmorContents();

        for (ItemStack armor : armors) {
            if (armor != null) {
                hash = extendInt(hash, armor.getType().toString().hashCode());
                Map<Enchantment, Integer> enchantments = armor.getEnchantments();

                if (!enchantments.isEmpty()) {
                    Map<Integer, Integer> map = new TreeMap<>();

                    for (Map.Entry<Enchantment, Integer> entry : enchantments.entrySet()) {
                        int key = entry.getKey().toString().hashCode();
                        map.put(
                                key,
                                (key * SpartanBukkit.hashCodeMultiplier) + entry.getValue()
                        );
                    }
                    for (int value : map.values()) {
                        hash = extendInt(hash, value);
                    }
                } else {
                    hash = extendInt(hash, falseHash);
                }
            } else {
                hash = extendInt(hash, falseHash);
            }
        }
        return hash;
    }

    public static int hashEnvironment(SpartanPlayer player,
                                      SpartanLocation location) {
        int hash = 1;
        boolean foundBlock = false;

        for (int y = -AlgebraUtils.integerCeil(PlayerUtils.totalJumpingMotion);
             y <= AlgebraUtils.integerCeil(player.getEyeHeight()); y++) {
            for (SpartanLocation cloned : location.clone().getSurroundingLocations(1.0, y, 1.0, true)) {
                SpartanBlock block = cloned.getBlock();
                Material material = block.material;

                if (BlockUtils.isSolid(material)) {
                    foundBlock = true;
                    hash = extendInt(hash, GroundUtils.getHeightsHash(material, trueHash));
                    hash = extendInt(hash, Double.hashCode(
                            AlgebraUtils.cut(AlgebraUtils.getHorizontalDistance(location, cloned), 1))
                    );

                    if (BlockUtils.areBouncingBlocks(material)) {
                        hash = extendInt(
                                hash,
                                BlockUtils.areBeds(material)
                                        ? bedWordHash
                                        : material.toString().hashCode()
                        );
                    } else if (BlockUtils.areIceBlocks(material)) {
                        hash = extendInt(hash, material.toString().hashCode());
                    }
                } else if (block.isLiquidOrWaterLogged()) {
                    foundBlock = true;
                    hash = extendInt(hash, material.toString().hashCode());
                }
                hash = extendInt(hash, Boolean.hashCode(block.waterLogged));
            }
        }

        if (foundBlock) {
            double box = location.getY() - location.getBlockY();

            if (GroundUtils.heightExists(box)) {
                hash = extendInt(hash, Double.hashCode(box));
            } else {
                hash = extendInt(hash, Double.hashCode(AlgebraUtils.cut(box, 2)));
            }
        }
        hash = extendInt(hash, AlgebraUtils.roundToNearest(player.movement.getTicksOnAir(), (int) TPS.maximum));
        return hash;
    }
}
