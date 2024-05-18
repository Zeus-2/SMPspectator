package name.modid;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SpectatorModeHandler {

    private static final long COMBAT_COOLDOWN = 60000; // 1 minute cooldown
    private static final long COOLDOWN_DURATION = 5000; // 5 seconds for general cooldown
    private Map<UUID, Long> lastCombatTimes = new HashMap<>();
    private Map<UUID, Long> lastSwitchTimes = new HashMap<>();

    public boolean conditionsToEnterSpectator(ServerPlayerEntity player) {
        UUID playerId = player.getUuid();

        // Check for recent combat activity
        if (recentlyInCombat(player)) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode shortly after being in combat.").formatted(Formatting.RED));
            return false;
        }

        // Check for a cooldown on switching modes
        if (onCooldown(player)) {
            player.sendMessage(Text.literal("You must wait before toggling spectator mode again.").formatted(Formatting.RED));
            return false;
        }

        // Check if the player is near a portal
        if (isNearPortal(player)) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode near a portal.").formatted(Formatting.RED));
            return false;
        }

        // Check if the player is holding or using restricted items
        if (isUsingRestrictedItems(player)) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode while using restricted items.").formatted(Formatting.RED));
            return false;
        }

        // Check for negative status effects
        if (hasNegativeEffects(player)) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode while affected by negative status effects.").formatted(Formatting.RED));
            return false;
        }

        // Update the last switch time
        lastSwitchTimes.put(playerId, System.currentTimeMillis());
        return true;
    }

    private boolean recentlyInCombat(ServerPlayerEntity player) {
        Long lastCombatTime = lastCombatTimes.getOrDefault(player.getUuid(), 0L);
        return System.currentTimeMillis() - lastCombatTime < COMBAT_COOLDOWN;
    }

    private boolean onCooldown(ServerPlayerEntity player) {
        Long lastSwitchTime = lastSwitchTimes.getOrDefault(player.getUuid(), 0L);
        return System.currentTimeMillis() - lastSwitchTime < COOLDOWN_DURATION;
    }

    private boolean isNearPortal(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        World world = player.getWorld();
        int range = 5;
        for (int dx = -range; dx <= range; dx++) {
            for (int dy = -range; dy <= range; dy++) {
                for (int dz = -range; dz <= range; dz++) {
                    BlockPos checkPos = pos.add(dx, dy, dz);
                    if (world.getBlockState(checkPos).isIn(BlockTags.PORTALS)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isUsingRestrictedItems(ServerPlayerEntity player) {
        return player.isUsingItem() && (player.getMainHandStack().getItem() != null ||
                player.getMainHandStack().getItem() instanceof CrossbowItem ||
                player.getMainHandStack().getItem() instanceof TridentItem);
    }

    private boolean hasNegativeEffects(ServerPlayerEntity player) {
        return player.hasStatusEffect(StatusEffects.POISON) ||
                player.hasStatusEffect(StatusEffects.WEAKNESS) ||
                player.hasStatusEffect(StatusEffects.SLOWNESS);
    }
}
