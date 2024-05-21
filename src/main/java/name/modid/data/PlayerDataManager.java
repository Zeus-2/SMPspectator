package name.modid.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.server.MinecraftServer;

import java.io.*;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerDataManager {
    private static final File DATA_FILE = new File("config/smpspectator/data.json");

    public static void loadData() {
        if (DATA_FILE.exists()) {
            try (Reader reader = new FileReader(DATA_FILE)) {
                Gson gson = createGson();
                Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
                Map<UUID, PlayerData> loadedData = gson.fromJson(reader, type);
                if (loadedData != null) {
                    loadedData.forEach((uuid, playerData) -> {
                        // No action needed, just loading data
                    });
                } else {
                    System.out.println("No player data found in the file.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Data file not found: " + DATA_FILE);
        }
    }

    public static void saveData() {
        try (Writer writer = new FileWriter(DATA_FILE)) {
            Gson gson = createGson();
            Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            gson.toJson(loadAllPlayerData(), type, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void enterSpectator(ServerPlayerEntity player) {
        PlayerData data = new PlayerData(
                player.getX(), player.getY(), player.getZ(),
                player.getYaw(), player.getPitch(),
                player.getWorld().getRegistryKey().getValue().toString()
        );
        savePlayerData(player.getUuid(), data);
        player.changeGameMode(GameMode.SPECTATOR);
        player.sendMessage(createMessage("You are now in SPECTATOR mode.", Formatting.AQUA), false);
    }

    public static void returnToSurvival(ServerPlayerEntity player) {
        PlayerData data = loadPlayerData(player.getUuid());
        if (data != null) {
            ServerWorld targetWorld = getServerWorld(player.getServer(), data.getWorld());
            if (targetWorld != null) {
                player.teleport(targetWorld, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
                player.changeGameMode(GameMode.SURVIVAL);
                deletePlayerData(player.getUuid());
                player.sendMessage(createMessage("You are now in SURVIVAL mode.", Formatting.GREEN), false);
            } else {
                player.sendMessage(Text.literal("Target world is null; cannot teleport player.").formatted(Formatting.RED));
            }
        } else {
            player.sendMessage(Text.literal("No original position data found; cannot return to SURVIVAL mode.").formatted(Formatting.RED));
        }
    }

    private static Text createMessage(String message, Formatting color) {
        return Text.literal(message).formatted(color);
    }

    private static Gson createGson() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }

    private static ServerWorld getServerWorld(MinecraftServer server, String worldId) {
        for (ServerWorld world : server.getWorlds()) {
            if (world.getRegistryKey().getValue().toString().equals(worldId)) {
                return world;
            }
        }
        return null;
    }

    private static void savePlayerData(UUID playerId, PlayerData data) {
        Map<UUID, PlayerData> playerDataMap = loadAllPlayerData();
        playerDataMap.put(playerId, data);
        saveAllPlayerData(playerDataMap);
    }

    private static PlayerData loadPlayerData(UUID playerId) {
        Map<UUID, PlayerData> playerDataMap = loadAllPlayerData();
        return playerDataMap.get(playerId);
    }

    private static void deletePlayerData(UUID playerId) {
        Map<UUID, PlayerData> playerDataMap = loadAllPlayerData();
        playerDataMap.remove(playerId);
        saveAllPlayerData(playerDataMap);
    }

    private static Map<UUID, PlayerData> loadAllPlayerData() {
        if (DATA_FILE.exists()) {
            try (Reader reader = new FileReader(DATA_FILE)) {
                Gson gson = createGson();
                Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
                Map<UUID, PlayerData> loadedData = gson.fromJson(reader, type);
                return loadedData != null ? loadedData : new HashMap<>();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new HashMap<>();
    }

    private static void saveAllPlayerData(Map<UUID, PlayerData> playerDataMap) {
        try (Writer writer = new FileWriter(DATA_FILE)) {
            Gson gson = createGson();
            Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
            gson.toJson(playerDataMap, type, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
