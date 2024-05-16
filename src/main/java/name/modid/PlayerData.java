package name.modid;

import net.minecraft.util.Identifier;
import com.google.gson.annotations.Expose;

import java.util.UUID;

public class PlayerData {
    @Expose private double x, y, z;
    @Expose private float yaw, pitch;
    @Expose private Identifier world;

    public PlayerData(double x, double y, double z, float yaw, float pitch, Identifier world) {
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

    public void update(double x, double y, double z, float yaw, float pitch, Identifier world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }
}
