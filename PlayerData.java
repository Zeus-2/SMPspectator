package name.modid;

import com.google.gson.annotations.Expose;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

public class PlayerData {
    @Expose private double x, y, z;
    @Expose private float yaw, pitch;
    @Expose private RegistryKey<World> world;  // Use RegistryKey<World> instead of Identifier

    public PlayerData(double x, double y, double z, float yaw, float pitch, RegistryKey<World> world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public float getYaw() {
        return yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public RegistryKey<World> getWorld() {
        return world;
    }

    public void update(double x, double y, double z, float yaw, float pitch, RegistryKey<World> world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }
}
