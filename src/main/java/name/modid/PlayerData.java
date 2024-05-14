package name.modid;

import net.minecraft.util.Identifier;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerData {
    private double x, y, z;
    private float yaw, pitch;
    private Identifier world;
    private static final Yaml yaml;

    static {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);

        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Representer representer = new Representer(dumperOptions);
        representer.addClassTag(PlayerData.class, new Tag("!playerdata"));

        Constructor constructor = new Constructor(loaderOptions);
        TypeDescription playerDataDesc = new TypeDescription(PlayerData.class, new Tag("!playerdata"));
        constructor.addTypeDescription(playerDataDesc);

        yaml = new Yaml(constructor, representer);
    }

    public PlayerData() {
    }

    public PlayerData(double x, double y, double z, float yaw, float pitch, Identifier world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    public void update(double x, double y, double z, float yaw, float pitch, Identifier world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.world = world;
    }

    public static Map<UUID, PlayerData> load(Reader reader) {
        Map<UUID, PlayerData> dataMap = yaml.load(reader);
        if (dataMap == null) {
            dataMap = new HashMap<>();
        }
        return dataMap;
    }

    public static void save(Writer writer, Map<UUID, PlayerData> data) {
        yaml.dump(data, writer);
    }

    // Getters and Setters for serialization
    public double getX() { return x; }
    public double getY() { return y; }
    public double getZ() { return z; }
    public float getYaw() { return yaw; }
    public float getPitch() { return pitch; }
    public Identifier getWorld() { return world; }

    // Setters for deserialization
    public void setX(double x) { this.x = x; }
    public void setY(double y) { this.y = y; }
    public void setZ(double z) { this.z = z; }
    public void setYaw(float yaw) { this.yaw = yaw; }
    public void setPitch(float pitch) { this.pitch = pitch; }
    public void setWorld(Identifier world) { this.world = world; }
}
