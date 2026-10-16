package ai.idealistic.spartan.abstraction.event;

import com.comphenix.protocol.wrappers.BlockPosition;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.Material;
import org.bukkit.World;

public class ServerBlockChange {

    public final BlockPosition position;
    @Setter
    @Getter
    private Material data;
    public int tick;
    @Getter
    private final World world;

    public ServerBlockChange(BlockPosition position, Material data, World world) {
        this.position = position;
        this.data = data;
        this.tick = 2;
        this.world = world;
    }

    public long generateHash() {
        return (this.position.getX() * 2L)
                + (this.position.getY())
                * (this.position.getZ() / 2L);
    }
}