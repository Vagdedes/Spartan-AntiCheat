package com.vagdedes.spartan.abstraction.player;

import com.vagdedes.spartan.abstraction.data.Trackers;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.TPS;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.entity.CombatUtils;
import com.vagdedes.spartan.utils.minecraft.inventory.EnchantmentUtils;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;

public class SpartanPlayerDamage {

    private static final EntityDamageEvent.DamageCause WORLD_BORDER = CombatUtils.findDamageCause("WORLD_BORDER");

    // Separator

    private final SpartanPlayer parent;
    public final long time;
    public final EntityDamageEvent event;
    public final SpartanLocation location;
    private final ItemStack activeItem;
    public final boolean boss, explosive;

    SpartanPlayerDamage(SpartanPlayer player) {
        this.parent = player;
        this.time = 0L;
        this.event = null;
        this.location = player.movement.getLocation();
        this.activeItem = null;
        this.boss = false;
        this.explosive = false;
    }

    SpartanPlayerDamage(SpartanPlayer player, EntityDamageEvent event) {
        boolean abstractVelocity = false;
        player.trackers.add(Trackers.TrackerType.DAMAGE, (int) TPS.maximum);
        this.parent = player;
        this.location = player.movement.getLocation();
        this.time = System.currentTimeMillis();
        this.event = event;

        if (event instanceof EntityDamageByEntityEvent) {
            EntityDamageByEntityEvent actualEvent = (EntityDamageByEntityEvent) this.event;

            if (actualEvent.getDamager() instanceof Player) {
                this.activeItem = ((Player) actualEvent.getDamager()).getInventory().getItemInHand();
                this.boss = false;
                this.explosive = false;
            } else if (actualEvent.getDamager() instanceof LivingEntity) {
                this.activeItem = ((LivingEntity) actualEvent.getDamager()).getEquipment().getItemInHand();
                this.boss = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)
                        && actualEvent.getDamager() instanceof Boss;
                this.explosive = false;
            } else if (actualEvent.getDamager() instanceof Projectile) {
                Projectile projectile = (Projectile) actualEvent.getDamager();

                if (projectile.getShooter() instanceof LivingEntity) {
                    LivingEntity shooter = (LivingEntity) projectile.getShooter();
                    this.activeItem = shooter.getEquipment().getItemInHand();
                    this.boss = MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)
                            && shooter instanceof Boss;

                    if (this.activeItem != null
                            && (this.activeItem.getType() == Material.BOW
                            || MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_14)
                            && this.activeItem.getType() == Material.CROSSBOW)) {
                        int level = this.activeItem.getEnchantmentLevel(EnchantmentUtils.ARROW_KNOCKBACK);

                        if (level > 2) {
                            this.parent.trackers.add(
                                    Trackers.TrackerType.ABSTRACT_VELOCITY,
                                    AlgebraUtils.integerRound(Math.log(level) * TPS.maximum)
                            );
                            abstractVelocity = true;
                        }
                    }
                } else {
                    this.activeItem = null;
                    this.boss = false;
                }
                this.explosive = false;
            } else if (actualEvent.getDamager() instanceof Explosive) {
                this.activeItem = null;
                this.boss = false;
                this.explosive = true;
            } else {
                this.activeItem = null;
                this.boss = false;
                this.explosive = false;
            }
        } else {
            this.activeItem = null;
            this.boss = false;
            this.explosive = false;
        }

        if (this.activeItem != null) {
            int level = this.activeItem.getEnchantmentLevel(Enchantment.KNOCKBACK);

            if (level > 2) {
                this.parent.trackers.add(
                        Trackers.TrackerType.ABSTRACT_VELOCITY,
                        AlgebraUtils.integerRound(Math.log(level) * TPS.maximum)
                );
                abstractVelocity = true;
            }
        }

        if (!abstractVelocity && !event.isCancelled()) {
            player.trackers.disable(Trackers.TrackerType.ABSTRACT_VELOCITY, 2);
        }
    }

    public EntityDamageByEntityEvent getEntityDamageByEntityEvent() {
        return this.event != null && this.event instanceof EntityDamageByEntityEvent
                ? (EntityDamageByEntityEvent) this.event
                : null;
    }

    public long timePassed() {
        return System.currentTimeMillis() - this.time;
    }

    public ItemStack getActiveItem() {
        return this.activeItem == null
                ? new ItemStack(Material.AIR)
                : this.activeItem;
    }

}
