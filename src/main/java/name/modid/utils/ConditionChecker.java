package name.modid.utils;

import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class ConditionChecker {

    public static boolean canEnterSpectator(ServerPlayerEntity player) {
        // Check if the player is aiming with a weapon
        boolean isAiming = (player.getMainHandStack().getItem() instanceof BowItem && player.isUsingItem()) ||
                (player.getMainHandStack().getItem() instanceof CrossbowItem && player.isUsingItem() && CrossbowItem.isCharged(player.getMainHandStack())) ||
                (player.getMainHandStack().getItem() instanceof TridentItem && player.isUsingItem());

        if (isAiming) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode while aiming a weapon.").formatted(Formatting.RED));
            return false;
        }

        // Check if the player has an animal on a leash
        boolean hasAnimalOnLeash = !player.getWorld().getEntitiesByClass(MobEntity.class, player.getBoundingBox().expand(10),
                entity -> entity.getHoldingEntity() == player).isEmpty();

        if (hasAnimalOnLeash) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode while having an animal on a leash.").formatted(Formatting.RED));
            return false;
        }

        // Check for negative status effects
        boolean hasNegativeEffect = player.hasStatusEffect(StatusEffects.POISON) ||
                player.hasStatusEffect(StatusEffects.WEAKNESS) ||
                player.hasStatusEffect(StatusEffects.SLOWNESS);

        if (hasNegativeEffect) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode while affected by negative status effects.").formatted(Formatting.RED));
            return false;
        }

        // Check if the player's health is sufficient
        if (player.getHealth() < 8.0f) {
            player.sendMessage(Text.literal("You need at least 4 hearts to switch to spectator mode.").formatted(Formatting.RED));
            return false;
        }

        // Check if the player is on the ground
        if (!player.isOnGround()) {
            player.sendMessage(Text.literal("You must be touching the ground to switch to spectator mode.").formatted(Formatting.RED));
            return false;
        }

        // Check if the player is near a portal
        if (isNearPortal(player)) {
            player.sendMessage(Text.literal("You cannot switch to spectator mode near a portal.").formatted(Formatting.RED));
            return false;
        }

        return true;
    }

    private static boolean isNearPortal(ServerPlayerEntity player) {
        BlockPos pos = player.getBlockPos();
        World world = player.getWorld();
        int range = 2; // Check within a 3-block radius

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
}
