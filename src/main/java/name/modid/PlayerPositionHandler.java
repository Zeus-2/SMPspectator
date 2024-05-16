package name.modid;  // Ensure this matches the package structure of your mod

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerPositionHandler {
    private static final Map<UUID, Long> lastMessageTimes = new HashMap<>();
    private static final long MESSAGE_COOLDOWN = 5000; // Cooldown time in milliseconds (5000ms = 5 seconds)

    public static void register() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                checkAndCorrectPlayerPosition(player);
            }
        });
    }

    private static void checkAndCorrectPlayerPosition(ServerPlayerEntity player) {
        if (player.getY() < -64.0) {
            World world = player.getWorld();
            if (!(world instanceof ServerWorld)) {
                System.out.println("World is not an instance of ServerWorld");
                return;
            }
            ServerWorld serverWorld = (ServerWorld) world;
            Vec3d correctedPosition = new Vec3d(player.getX(), -63.0, player.getZ());

            // Use the casted ServerWorld in the teleport call
            player.teleport(serverWorld, correctedPosition.x, correctedPosition.y, correctedPosition.z, player.getYaw(), player.getPitch());
            player.fallDistance = 0.0f; // Reset fall distance

            // Check for message cooldown
            long currentTime = System.currentTimeMillis();
            lastMessageTimes.putIfAbsent(player.getUuid(), 0L); // Initialize if not present
            if (currentTime - lastMessageTimes.get(player.getUuid()) > MESSAGE_COOLDOWN) {
                player.sendMessage(Text.literal("You cannot go below Y=-64."), false);
                lastMessageTimes.put(player.getUuid(), currentTime); // Update last message time
            }
        }
    }
}
