package name.modid.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final String DATA_FILE_PATH = "playerData.json";
    private static final Map<UUID, PlayerData> playerData = new HashMap<>();
    private static final Gson gson = new Gson();

    public static void savePlayerData(ServerPlayerEntity player) {
        PlayerData data = new PlayerData(player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(), player.getServerWorld().getRegistryKey().getValue().toString());
        playerData.put(player.getUuid(), data);
        saveToFile(); // Save every time a change occurs
    }

    public static void loadPlayerData(ServerPlayerEntity player, MinecraftServer server) {
        PlayerData data = playerData.get(player.getUuid());
        if (data != null) {
            Identifier worldId = Identifier.tryParse(data.getWorld());
            // Iterate through all worlds to find the matching world
            for (ServerWorld world : server.getWorlds()) {
                if (world.getRegistryKey().getValue().equals(worldId)) {
                    player.teleport(world, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
                    break; // Exit after finding the correct world and teleporting the player
                }
            }
        }
    }


    public static void saveToFile() {
        try (FileWriter writer = new FileWriter(DATA_FILE_PATH)) {
            gson.toJson(playerData, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void loadFromFile() {
        try (FileReader reader = new FileReader(DATA_FILE_PATH)) {
            Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            Map<UUID, PlayerData> data = gson.fromJson(reader, type);
            if (data != null) {
                playerData.clear();
                playerData.putAll(data);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
