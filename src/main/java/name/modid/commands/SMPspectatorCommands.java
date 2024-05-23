package name.modid.commands;

import name.modid.SMPspectator;
import name.modid.config.ConfigManager;
import name.modid.permissions.PermissionHandler;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;

public class SMPspectatorCommands {

    public static void registerCommands() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            register(dispatcher);
        });
    }

    private static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("s")
                .executes(context -> {
                    ServerPlayerEntity player = context.getSource().getPlayer();
                    if (ConfigManager.config.enabled) {
                        assert player != null;
                        SMPspectator.toggleSpectatorMode(player);
                    } else {
                        context.getSource().sendMessage(Text.literal("Spectator mode toggling is currently disabled.").formatted(Formatting.RED));
                    }
                    return 1;
                })
                .then(CommandManager.literal("force")
                        .requires(src -> ConfigManager.config.enabled && PermissionHandler.hasPermission(src, "smpspectator.force"))
                        .then(CommandManager.argument("target", EntityArgumentType.player())
                                .executes(context -> {
                                    ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
                                    SMPspectator.toggleSpectatorMode(target);
                                    return 1;
                                })
                        )
                )
                .then(CommandManager.literal("enable")
                        .requires(src -> !ConfigManager.config.enabled && PermissionHandler.hasPermission(src, "smpspectator.enable"))
                        .executes(context -> {
                            ConfigManager.config.enabled = true;
                            ConfigManager.saveConfig();
                            refreshCommandsForAllPlayers(context.getSource());
                            context.getSource().sendMessage(Text.literal("Spectator toggle is now enabled").formatted(Formatting.GREEN));
                            return 1;
                        })
                )
                .then(CommandManager.literal("disable")
                        .requires(src -> ConfigManager.config.enabled && PermissionHandler.hasPermission(src, "smpspectator.disable"))
                        .executes(context -> {
                            ConfigManager.config.enabled = false;
                            ConfigManager.saveConfig();
                            refreshCommandsForAllPlayers(context.getSource());
                            context.getSource().sendMessage(Text.literal("Spectator toggle is now disabled").formatted(Formatting.RED));
                            return 1;
                        })
                )
                .then(CommandManager.literal("reload")
                        .requires(src -> PermissionHandler.hasPermission(src, "smpspectator.reload"))
                        .executes(context -> {
                            ConfigManager.loadConfig();
                            refreshCommandsForAllPlayers(context.getSource());
                            context.getSource().sendMessage(Text.literal("Config reloaded").formatted(Formatting.LIGHT_PURPLE));
                            return 1;
                        })
                )
                .then(CommandManager.literal("effect")
                        .requires(src -> PermissionHandler.hasPermission(src, "SMPspectator.effect"))
                        .executes(context -> {
                            ServerPlayerEntity player = context.getSource().getPlayer();
                            if (player != null) {
                                giveNightVision(player);
                                context.getSource().sendMessage(Text.literal("Infinite Night Vision added.").formatted(Formatting.BLUE));
                            }
                            return 1;
                        })
                )
                .then(CommandManager.literal("speed")
                        .requires(src -> ConfigManager.config.enabled && PermissionHandler.hasPermission(src, "smpspectator.speed"))
                        .then(CommandManager.argument("speed", IntegerArgumentType.integer(1, ConfigManager.config.maxSpeed))
                                .executes(context -> {
                                    ServerPlayerEntity player = context.getSource().getPlayer();
                                    assert player != null;
                                    if (player.interactionManager.getGameMode() != GameMode.SPECTATOR) {
                                        context.getSource().sendMessage(Text.literal("This command is only available in Spectator mode.").formatted(Formatting.RED));

                                        return 0;
                                    }
                                    int speed = IntegerArgumentType.getInteger(context, "speed");
                                    setPlayerSpeed(player, speed);
                                    return 1;
                                })
                        )
                )
        );
    }

    private static void refreshCommandsForAllPlayers(ServerCommandSource source) {
        source.getServer().getPlayerManager().getPlayerList().forEach(
                player -> source.getServer().getCommandManager().sendCommandTree(player)
        );
    }

    private static void setPlayerSpeed(ServerPlayerEntity player, int speed) {
        if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
            float baseSpeed = 0.10f;
            float newSpeed = baseSpeed * speed;
            player.getAbilities().setFlySpeed(newSpeed);
            player.sendAbilitiesUpdate();
            player.sendMessage(Text.literal("Spectator fly speed set to " + speed + "."), false);
        } else {
            player.sendMessage(Text.literal("This command is only available in Spectator mode.").formatted(Formatting.RED), false);
        }
    }

    private static void giveNightVision(ServerPlayerEntity player) {
        // Check if the player is in Spectator mode
        if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
            StatusEffectInstance effect = new StatusEffectInstance(StatusEffects.NIGHT_VISION, Integer.MAX_VALUE, 1, false, false, true);
            player.addStatusEffect(effect);
            player.sendMessage(Text.literal("Night Vision granted.").formatted(Formatting.GREEN));
        } else {
            player.sendMessage(Text.literal("Night Vision can only be granted in Spectator mode.").formatted(Formatting.RED));
        }
    }
}
