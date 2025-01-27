package com.vagdedes.spartan.abstraction.data;

import com.vagdedes.spartan.abstraction.protocol.PlayerProtocol;
import com.vagdedes.spartan.abstraction.world.SpartanBlock;
import com.vagdedes.spartan.abstraction.world.SpartanLocation;
import com.vagdedes.spartan.functionality.concurrent.CheckThread;
import com.vagdedes.spartan.functionality.server.MultiVersion;
import com.vagdedes.spartan.utils.minecraft.world.BlockUtils;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;

@Data
@RequiredArgsConstructor
public class EncirclementData {
    private boolean ice = false;
    private boolean slime = false;
    private boolean slimeWide = false;
    private boolean slimeHeight = false;
    private boolean water = false;
    private boolean semi = false;
    private boolean glide = false;
    private boolean jumpModify = false;
    private boolean climb = false;
    private boolean bubble = false;

    public EncirclementData(PlayerProtocol protocol) {
        Location location = (protocol.bukkitExtra.getVehicle() == null) ?
                        protocol.getLocation()
                        : protocol.bukkitExtra.getVehicle().getLocation();
        CheckThread.run(() -> {
            double x = location.getX();
            double y = location.getY() - 0.1;
            double z = location.getZ();

            for (int dx = -2; dx <= 2; ++dx) {
                for (int dy = -2; dy <= 2; ++dy) {
                    for (int dz = -2; dz <= 2; ++dz) {
                        boolean h = Math.abs(dx) < 2 && Math.abs(dz) < 2;
                        SpartanBlock b = new SpartanLocation(
                                new Location(protocol.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                        ).getBlock();
                        Material material = protocol.packetWorld.getBlock(
                                new Location(protocol.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                        );
                        Material materialWide = protocol.packetWorld.getBlock(
                                        new Location(protocol.getWorld(), x + (double) dx * 0.5, y + (double) (dy * 0.5) - 0.3, z + (double) dz * 0.5)
                        );
                        Material materialTop = protocol.packetWorld.getBlock(
                                        new Location(protocol.getWorld(), x + (double) dx * 0.5, y + (double) (dy * 0.5) + 1.0, z + (double) dz * 0.5)
                        );
                        Material materialFrom = protocol.packetWorld.getBlock(
                                new Location(protocol.getWorld(), x + (double) dx * 0.3, y + (double) dy * 0.5, z + (double) dz * 0.3)
                        );
                        Material m2 = b.getType();
                        if (material == null || materialFrom == null || m2 == null) return;

                        if ((BlockUtils.areHoneyBlocks(material)
                                || BlockUtils.areBeds(material)
                                || BlockUtils.areSlimeBlocks(material))) {
                            this.slime = true;
                        }
                        if (Math.abs(dx) < 2
                                && Math.abs(dz) < 2
                                && (BlockUtils.areInteractiveBushes(material)
                                || BlockUtils.areBeds(material)
                                || BlockUtils.isPowderSnow(material))) {
                            this.jumpModify = true;
                        }
                        if (BlockUtils.canClimb(material, false) || BlockUtils.canClimb(materialTop, false)) {
                            this.climb = true;
                        }
                        if (Math.abs(dy) < 2 && h
                                && (BlockUtils.isPowderSnow(material)
                                        || (BlockUtils.isPowderSnow(materialTop)
                                || BlockUtils.areHoneyBlocks(material)
                                || BlockUtils.areWebs(material) || BlockUtils.areWebs(materialTop)
                                || BlockUtils.areInteractiveBushes(material)
                                        || BlockUtils.areInteractiveBushes(materialTop)))) {
                            this.glide = true;
                        }
                        if (!BlockUtils.canClimb(material, false) &&
                                (BlockUtils.isSemiSolid(material)
                                                || BlockUtils.isSemiSolid(materialFrom)
                                                || BlockUtils.isSemiSolid(materialWide)
                                        || BlockUtils.areWalls(materialFrom)
                                        || BlockUtils.areCobbleWalls(materialFrom)
                                        || BlockUtils.areSlimeBlocks(material)
                                        || BlockUtils.areHoneyBlocks(material)
                                        || BlockUtils.areWebs(material)
                                                || BlockUtils.areWebs(materialTop))) {
                            this.semi = true;
                        }
                        if (MultiVersion.isOrGreater(MultiVersion.MCVersion.V1_13)
                                && (material.equals(Material.BUBBLE_COLUMN) || m2.equals(Material.BUBBLE_COLUMN))) {
                            this.bubble = true;
                        }
                        if (BlockUtils.isLiquidOrWaterLogged(b, true)
                                || BlockUtils.isLiquid(protocol.packetWorld.getBlock(
                                new Location(protocol.getWorld(), x + (double) dx * 0.3, y, z + (double) dz * 0.3)
                        ))
                                || BlockUtils.isLiquid(material)
                                || BlockUtils.isLiquid(m2)
                                || BlockUtils.isWaterLogged(b)) {

                            this.water = true;
                        }
                        if (BlockUtils.areIceBlocks(material)) {
                            this.ice = true;
                        }
                        {
                            double xF = protocol.getFromLocation().getX();
                            double yF = protocol.getFromLocation().getY();
                            double zF = protocol.getFromLocation().getZ();
                            Material materialBig = protocol.packetWorld.getBlock(
                                    new Location(protocol.getWorld(), x + (double) dx, y + (double) dy, z + (double) dz)
                            );
                            Material materialBigFrom = protocol.packetWorld.getBlock(
                                            new Location(protocol.getWorld(), xF + (double) dx, yF + (double) dy + 0.5, zF + (double) dz)
                            );
                            if (materialBig == null) return;
                            if ((BlockUtils.areHoneyBlocks(materialBig)
                                    || BlockUtils.areBeds(materialBig)
                                    || BlockUtils.areSlimeBlocks(materialBigFrom)
                                    || BlockUtils.areSlimeBlocks(materialBig))) {
                                this.slimeHeight = true;
                                this.slimeWide = true;
                            }
                        }
                    }
                }
            }
        });
    }
    public boolean isAllFalse() {
        return !(isIce() || isSemi() || isSlime() || isSlimeHeight() || isWater() || isJumpModify() || isBubble());
    }
}
