package com.vagdedes.spartan.abstraction.protocol;

import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attribute;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.Attributes;
import com.vagdedes.spartan.abstraction.check.implementation.movement.simulation.modules.engine.motion.*;
import com.vagdedes.spartan.abstraction.player.SpartanPotionEffect;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.MathHelper;
import com.vagdedes.spartan.utils.math.RayUtils;
import com.vagdedes.spartan.utils.minecraft.vector.SpartanVector2F;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.List;

public class MCClient {

    public float jumpMovementFactor;
    public int onAir, onGroundTicks;
    public long lastPacketTime;
    public Player player;
    public org.bukkit.util.Vector veloClaim;
    private final List<MotionVector> predicted;
    public boolean isAirBorne, isJumping;
    public MotionVector motion;
    public MotionVector velocityQuery = null;
    public int verifiedPacket, veloTicker;
    public MotionVector verifiedMotion;
    private final List<MotionVector> vectors = new ArrayList<>();
    private final List<KeyBind> binds = new ArrayList<>();

    public MotionVector getMotion() {
        return motion;
    }

    public void setMotion(MotionVector motion) {
        this.motion = motion;
    }

    public void setMotion(double x, double y, double z) {
        this.motion.setX(x);
        this.motion.setY(y);
        this.motion.setZ(z);
    }

    public MCClient(Player player) {
        this.player = player;
        this.isAirBorne = false;
        this.isJumping = false;
        this.veloClaim = null;
        this.onAir = 0;
        this.onGroundTicks = 0;
        this.motion = new MotionVector(0, 0, 0);
        this.jumpMovementFactor = 0.0F;
        this.predicted = new ArrayList<>();
        this.verifiedMotion = new MotionVector(0, 0, 0);
        this.verifiedPacket = 0;
        this.lastPacketTime = System.currentTimeMillis();
        this.veloTicker = 0;
        double[] values = {0.9800000190734863, 0.2940000295639038};

        for (double x : values) {
            for (double z : values) {
                vectors.add(new MotionVector(x, 0, z));
                vectors.add(new MotionVector(x, 0, -z));
                vectors.add(new MotionVector(-x, 0, z));
                vectors.add(new MotionVector(-x, 0, -z));
                vectors.add(new MotionVector(x, 0, 0));
                vectors.add(new MotionVector(-x, 0, 0));
                vectors.add(new MotionVector(0, 0, z));
                vectors.add(new MotionVector(0, 0, -z));
            }
        }
        vectors.add(new MotionVector(0, 0, 0));
        binds.add(new KeyBind(true, false, false, false));
        binds.add(new KeyBind(false, true, false, false));
        binds.add(new KeyBind(true, false, true, false));
        binds.add(new KeyBind(true, false, false, true));
        binds.add(new KeyBind(false, true, true, false));
        binds.add(new KeyBind(false, true, false, true));
        binds.add(new KeyBind(false, false, true, false));
        binds.add(new KeyBind(false, false, false, true));
        binds.add(new KeyBind(false, false, false, false));
    }

    public void movement(double dx, double dy, double dz) {
        this.predicted.clear();
        preTick(new MotionVector(dx, dy, dz));
        for (KeyBind bind : binds) {
            this.motion = this.verifiedMotion.cloneVector();
            tick(bind, new MotionVector(dx, dy, dz));
            this.predicted.add(this.motion);
        }
        veloClaim = null;
    }

    public MotionVector getNext(double dx, double dy, double dz) {
        this.predicted.clear();
        preTick(new MotionVector(dx, dy, dz));
        this.motion = this.verifiedMotion.cloneVector();
        tick(new KeyBind(false, false, false, false), new MotionVector(dx, dy, dz));
        return this.motion;
    }

    public SpartanProtocol p() {
        return SpartanBukkit.getProtocol(this.player);
    }

    public void preTick(MotionVector currentMotion) {
        boolean onGround = this.p().isOnGround();
        if (!onGround) {
            onAir++;
            onGroundTicks = 0;
        } else {
            onAir = 0;
            onGroundTicks++;
            isJumping = false;
            isAirBorne = false;
        }
    }

    public boolean checkCollide() {
        Location location = this.p().getLocation();

        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y = location.getBlockY() - 1; y <= location.getBlockY() + 1; y++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    SpartanBlock block = new SpartanLocation(location.getWorld(), x, y, z, 0.0f, 0.0f).getBlock();

                    if (block.material.isSolid() || ignore(block.material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkCollideSpecify() {
        Location location = this.p().getLocation();

        for (int x = location.getBlockX() - 1; x <= location.getBlockX() + 1; x++) {
            for (int y = location.getBlockY() - 1; y <= location.getBlockY() + 1; y++) {
                for (int z = location.getBlockZ() - 1; z <= location.getBlockZ() + 1; z++) {
                    SpartanBlock block = new SpartanLocation(location.getWorld(), x, y, z, 0.0f, 0.0f).getBlock();

                    if (ignore(block.material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean checkCollideOnTop() {
        Location location = this.p().getLocation();
        World world = location.getWorld();
        double startX = location.getX() - 0.3;
        double endX = location.getX() + 0.3;
        double startY = location.getY();
        double endY = location.getY() + 2;
        double startZ = location.getZ() - 0.3;
        double endZ = location.getZ() + 0.3;
        double increment = 0.3;

        for (double x = startX; x <= endX; x += increment) {
            for (double y = startY; y <= endY; y += 1) {
                for (double z = startZ; z <= endZ; z += increment) {
                    SpartanBlock block = new SpartanLocation(world, x, y, z, 0.0f, 0.0f).getBlock();

                    if (block.material.isSolid() || ignore(block.material)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean ignore(Material material) {
        return com.vagdedes.spartan.utils.minecraft.world.BlockUtils.isSemiSolid(material);
    }

    public void tick(KeyBind keyBind, MotionVector currentMotion) {
        boolean onGround = this.p().isOnGround();
        final double yaw = Math.toRadians(this.p().getLocation().getYaw());
        double xM = -Math.sin(yaw);
        double zM = Math.cos(yaw);
        if (!onGround) {
            { // air Y sim
                SpartanPotionEffect levitation = null;
                if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_9)) {
                    levitation = this.p().spartanPlayer.getPotionEffect(PotionEffectType.LEVITATION, 0);
                }
                double d0 = 0.08D;
                boolean flag = this.getMotion().y <= 0.0D;

                if (flag && MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13) && this.p().spartanPlayer.getPotionEffect(PotionEffectType.SLOW_FALLING, 0) != null) {
                    d0 = 0.01D;
                }
                Location blockpos = this.p().getLocation().clone().add(0, -1, 0);
                float f3 = BlockUtils.getSlipperiness(new SpartanLocation(blockpos).getBlock());
                MotionVector vector3d5 = this.revelantVec(new MotionVector(0, 0, 0));
                double d2 = vector3d5.y;
                if (levitation != null) {
                    d2 += (0.05D * (double) (levitation.bukkitEffect.getAmplifier() + 1) - vector3d5.y) * 0.2D;
                    //this.fallDistance = 0.0F;
                }
                if (!MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_10) || this.p().player.hasGravity()) {
                    d2 -= d0;
                }
                this.getMotion().y = d2 * (double) 0.98F;
                if (this.onAir == 6) {
                    /*
                    when jumping in the 6th tick, the speed change is different.
                     */
                    double mY = RayUtils.scaleVal(currentMotion.y, 4);
                    double pV = RayUtils.scaleVal(this.getMotion().getY(), 4);
                    if (keyBind.isNone()) {
                        if (mY == -0.0784 && pV == 0.0031) {
                            this.getMotion().y = -0.0784;
                        }
                        if (mY == 0.0 && pV == 0.0031) {
                            this.getMotion().y = 0.0;
                        }
                    }
                }
                if (onAir == 1 && currentMotion.y > 0 && veloClaim == null) {
                    jump();
                    this.isJumping = true;
                }
                if (this.checkCollideOnTop()) {
                    if (keyBind.isNone()) this.getMotion().y = -0.0784;
                    if (keyBind.is(true, false, false, false)
                            && this.getMotion().y > currentMotion.y) {
                        this.getMotion().y = RayUtils.scaleVal(currentMotion.getY(), 4);
                    }
                }
            }

            { // XZ sim on air
                if (this.onAir == 1 && this.veloTicker == 0) {
                    if (this.p().isSprinting()) {
                        this.motion.x = (keyBind.isNone()) ? getAirSprintMoveFactor().x * xM : getAirSprintMoveFactor().y * xM;
                        this.motion.z = (keyBind.isNone()) ? getAirSprintMoveFactor().x * zM : getAirSprintMoveFactor().y * zM;
                    } else {
                        this.motion.x = (keyBind.isNone()) ? getAirDefaultMoveFactor().x * xM : getAirDefaultMoveFactor().y * xM;
                        this.motion.z = (keyBind.isNone()) ? getAirDefaultMoveFactor().x * zM : getAirDefaultMoveFactor().y * zM;
                    }
                }
                if (this.onAir == 2) {
                    if (veloTicker > 0) {
                        this.motion.x = this.verifiedMotion.x / 1.831501942690627;
                        this.motion.z = this.verifiedMotion.z / 1.831501942690627;
                    } else {
                        this.motion.x = this.verifiedMotion.x / 1.701358475829172;
                        this.motion.z = this.verifiedMotion.z / 1.701358475829172;
                    }
                }
                if (this.onAir > 1) {
                    if (keyBind.is(true, false, false, false)) {
                        this.motion.x += getRelevantMoveFactor() * xM;
                        this.motion.z += getRelevantMoveFactor() * zM;
                    }
                    if (keyBind.isNone()) {
                        this.motion.x /= 1.2;
                        this.motion.z /= 1.2;
                    }
                }
                if (this.onAir > 2) {
                    this.motion.x = this.verifiedMotion.x / 1.098901066812599;
                    this.motion.z = this.verifiedMotion.z / 1.098901066812599;
                }
            }
        } else {

            { // onGround logic XZ
                if (this.onGroundTicks > 1 || keyBind.isFull()) {
                    this.motion.x = getRelevantMoveFactor() * xM;
                    this.motion.z = getRelevantMoveFactor() * zM;
                }
                if (keyBind.isNone()) {
                    this.motion.x /= 20;
                    this.motion.z /= 20;
                }
            }

            // Other
            if (!this.p().isOnGroundFrom() && currentMotion.y < this.motion.y
                    && Math.abs(currentMotion.y - this.motion.y) < 0.03) {
                this.motion.y = currentMotion.y;
            } else if (!this.p().isOnGroundFrom() && currentMotion.y > this.motion.y) {
                /*
                Let's prevent lagged block falses by allowing moving
                through the blocks at the bottom,
                because it's going to teleport them back anyway.
                 */
                this.motion.y = currentMotion.getY();
            } else {
                this.getMotion().y = 0;
            }
            if (keyBind.is(false, true, false, false)) {
                if (this.getMotion().y == 0 && onGroundTicks < 3
                        && RayUtils.scaleVal(currentMotion.y, 4) == -0.1216) {
                    this.getMotion().y = RayUtils.scaleVal(currentMotion.getY(), 4);
                }
            }
        }

        // Start of Velocity handle
        MotionVector velocityMotionMC = (this.velocityQuery == null) ? null : this.velocityQuery.cloneVector();
        MotionVector velocityMotionOT = (this.velocityQuery == null) ? null : this.motion.addWithClone(velocityQuery.getX(), velocityQuery.getY(), velocityQuery.getZ());
        if (keyBind.isFull() && velocityMotionMC != null
                && velocityMotionOT != null && (checkCollide() || onAir < 3)) {
            // Jumping after Velocity will cause value modifier
            velocityMotionMC.add(0, 0.42, 0);
            velocityMotionOT.add(0, 0.42, 0);
        }
        if (velocityMotionMC != null && currentMotion.compare(velocityMotionMC)) {
            this.setMotion(velocityMotionMC);
            this.velocityQuery = null;
        } else if (velocityMotionOT != null && currentMotion.compare(velocityMotionOT)) {
            this.setMotion(velocityMotionOT);
            this.velocityQuery = null;
        } else if (verifiedPacket > 3 && velocityQuery != null) {
            this.setMotion(velocityMotionMC);
            this.verifiedMotion = velocityMotionMC;
            this.velocityQuery = null;
            this.verifiedPacket = 0;
        } else if (keyBind.isNone() && keyBind.isFull() && velocityQuery != null) {
            this.setMotion(velocityMotionMC);
        }

        /*
        Specify actions
        Like collision
         */
        if (checkCollideSpecify() && keyBind.isNone()) {
            if (Math.abs(this.getMotion().y - currentMotion.y) <= 0.7) {
                this.getMotion().y = RayUtils.scaleVal(currentMotion.getY(), 4);
            }
        }

        if (veloTicker > 0) veloTicker--;
    }

    protected float getJumpUpwardsMotion() {
        return 0.42F; // * this.getJumpFactor();
    }

    public List<MotionVector> getPredicted() {
        return this.predicted;
    }

    public MotionVector revelantVec(MotionVector vector) {
        this.moveRelative(this.getRelevantMoveFactor(), vector);
        this.setMotion(this.handleOnClimbable(this.getMotion()));
        this.move(MoverType.SELF, this.getMotion());
        MotionVector vector3d = this.getMotion();

        if (this.isJumping && this.canClimb(this.p())) {
            vector3d = new MotionVector(vector3d.x, 0.2D, vector3d.z);
        }

        return vector3d;
    }

    private MotionVector handleOnClimbable(MotionVector vector) {
        if (this.canClimb(this.p())) {
            float f = 0.15F;
            double d0 = MathHelper.clamp_double(vector.x, (double) -0.15F, (double) 0.15F);
            double d1 = MathHelper.clamp_double(vector.z, (double) -0.15F, (double) 0.15F);
            double d2 = Math.max(vector.y, (double) -0.15F);

            vector = new MotionVector(d0, d2, d1);
        }

        return vector;
    }

    private boolean canClimb(SpartanLocation location) {
        Material material = location.getBlock().material;
        return com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(material, false)
                || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areTrapdoors(material)
                && (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, 1, 0).getBlock().material, false)
                || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, -1, 0).getBlock().material, false));
    }

    private boolean canClimb(SpartanProtocol protocol) {
        SpartanLocation location = new SpartanLocation(protocol.getLocation());
        Material material = location.getBlock().material;
        return com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(material, false)
                || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.areTrapdoors(material)
                && (com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, 1, 0).getBlock().material, false)
                || com.vagdedes.spartan.utils.minecraft.world.BlockUtils.canClimb(location.clone().add(0, -1, 0).getBlock().material, false));
    }

    public float getRelevantMoveFactor() {
        return (float) Attributes.getAttributeValue(Attribute.MOVEMENT_SPEED, this.p())
                * (0.21600002F) * 10;
    }

    public SpartanVector2F getAirSprintMoveFactor() {
        float v = (float) Attributes.getAttributeValue(Attribute.MOVEMENT_SPEED, this.p());
        return new SpartanVector2F(v * 10 * 0.3274F, v * 10 * 0.615342F);
    }

    public SpartanVector2F getAirDefaultMoveFactor() {
        float v = (float) Attributes.getAttributeValue(Attribute.MOVEMENT_SPEED, this.p());
        return new SpartanVector2F(0.0F, v * 10 * 0.280047F);
    }

    public void move(MoverType typeIn, MotionVector pos) {
        // for now - nothing
    }

    public void moveRelative(float friction, MotionVector relative) {
        StrafeEvent event = new StrafeEvent(friction, relative, this.p().getLocation().getYaw());
        MotionVector vector3d = getAbsoluteMotion(event.getRelative(), event.getFriction(), event.getYaw());
        this.getMotion().add(vector3d.x, vector3d.y, vector3d.z);
    }

    private static MotionVector getAbsoluteMotion(MotionVector relative, float friction, float yaw) {
        double d0 = relative.lengthSquared();

        if (d0 < 1.0E-7D) {
            return MotionVector.ZERO;
        } else {
            MotionVector vector3d = (d0 > 1.0D ? relative.normalize() : relative).scale(friction);
            float f = MathHelper.sin(yaw * ((float) Math.PI / 180F));
            float f1 = MathHelper.cos(yaw * ((float) Math.PI / 180F));
            return new MotionVector(vector3d.x * (double) f1 - vector3d.z * (double) f, vector3d.y, vector3d.z * (double) f1 + vector3d.x * (double) f);
        }
    }

    public void jump() {
        float f = this.getJumpUpwardsMotion();
        SpartanPotionEffect jumpEffect = null;
        if (PotionEffectType.getByName("JUMP_BOOST") != null) {
            jumpEffect = this.p().spartanPlayer.getPotionEffect(PotionEffectType.getByName("JUMP_BOOST"), 1L);
        } else {
            jumpEffect = this.p().spartanPlayer.getPotionEffect(PotionEffectType.getByName("JUMP"), 1L);
        }

        if (jumpEffect != null) {
            f += 0.1F * (float) (jumpEffect.bukkitEffect.getAmplifier() + 1);
        }

        MotionVector vector3d = this.getMotion();
        this.setMotion(vector3d.x, (double) f, vector3d.z);

        if (this.p().isSprinting()) {

            JumpEvent jumpEvent = new JumpEvent(this.p().getLocation().getYaw());

            float f1 = jumpEvent.getYaw() * ((float) Math.PI / 180F);
            this.getMotion().add((double) (-Math.sin(f1) * 0.2F), 0.0D, (double) (Math.cos(f1) * 0.2F));
        }

        this.isAirBorne = true;
    }

    public void claimVelocity(org.bukkit.util.Vector velocity) {
        this.velocityQuery = new MotionVector(velocity.getX(), velocity.getY(), velocity.getZ());
        this.veloClaim = velocity;
        this.verifiedPacket = 0;
        this.veloTicker = 2;
    }

    private void debug(String msg) {
        this.p().player.sendMessage(msg);
    }

}
