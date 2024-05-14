package name.modid;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SMPspectator implements ModInitializer {
	private static final Logger LOGGER = LogManager.getLogger();
	private static final File CONFIG_FILE = new File("config/smpspectator/config.yaml");
	private static final File DATA_FILE = new File("config/smpspectator/data.yaml");
	private static final Config config = new Config();
	private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private static LuckPerms luckPerms;

	@Override
	public void onInitialize() {
		ensureDirectoryExists();
		loadConfig();
		loadData();

		LOGGER.info("Initializing SMPspectator...");

		// Register commands regardless of LuckPerms state
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommands(dispatcher);
			LOGGER.info("Commands have been registered.");
		});

		// Initialize LuckPerms when the server starts
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				luckPerms = LuckPermsProvider.get();
				LOGGER.info("LuckPerms has been initialized.");
			} catch (IllegalStateException e) {
				LOGGER.warn("LuckPerms is not loaded or failed to initialize.", e);
			}
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> saveData());
		LOGGER.info("SMPspectator has been initialized.");
	}

	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("s")
				.requires(source -> hasPermission(source, "smpspectator.use"))
				.executes(this::executeToggle)
				.then(CommandManager.literal("force")
						.requires(source -> hasPermission(source, "smpspectator.force"))
						.then(CommandManager.argument("target", EntityArgumentType.player())
								.executes(context -> {
									ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
									toggleSpectatorMode(target);
									target.sendMessage(createMessage("You have been forced into/out of spectator mode by " + context.getSource().getName() + ".", Formatting.RED), false);
									context.getSource().sendMessage(createMessage("Forced " + target.getName().getString() + " into/out of spectator mode.", Formatting.RED));
									return 1;
								})
						)
				)
				.then(CommandManager.literal("enable")
						.requires(source -> hasPermission(source, "smpspectator.enable"))
						.executes(context -> {
							config.enabled = true;
							saveConfig();
							context.getSource().sendMessage(createMessage("SMPspectator enabled.", Formatting.GREEN));
							return 1;
						})
				)
				.then(CommandManager.literal("disable")
						.requires(source -> hasPermission(source, "smpspectator.enable"))
						.executes(context -> {
							config.enabled = false;
							saveConfig();
							context.getSource().sendMessage(createMessage("SMPspectator disabled.", Formatting.RED));
							return 1;
						})
				)
				.then(CommandManager.literal("reload")
						.requires(source -> hasPermission(source, "smpspectator.reload"))
						.executes(context -> {
							loadConfig();
							context.getSource().sendMessage(createMessage("SMPspectator config reloaded.", Formatting.YELLOW));
							return 1;
						})
				)
		);

		dispatcher.register(CommandManager.literal("speed")
				.requires(source -> hasPermission(source, "smpspectator.speed"))
				.then(CommandManager.argument("value", IntegerArgumentType.integer(1, config.maxSpeed))
						.executes(context -> {
							ServerPlayerEntity player = context.getSource().getPlayer();
							if (player != null && player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
								int speed = IntegerArgumentType.getInteger(context, "value");
								float flySpeed = speed / 10.0f;
								player.getAbilities().setFlySpeed(flySpeed);
								player.sendMessage(createMessage("Spectator speed set to " + speed, Formatting.YELLOW), false);
								player.sendAbilitiesUpdate();
							} else {
								context.getSource().sendError(createMessage("You must be in spectator mode to set speed.", Formatting.RED));
							}
							return 1;
						})
				)
		);
	}

	private boolean hasPermission(ServerCommandSource source, String permission) {
		if (source.hasPermissionLevel(2)) return true; // Default to allow for ops
		if (source.getEntity() instanceof ServerPlayerEntity) {
			ServerPlayerEntity player = (ServerPlayerEntity) source.getEntity();
			if (luckPerms != null) {
				User user = luckPerms.getUserManager().getUser(player.getUuid());
				if (user != null) {
					return user.getCachedData().getPermissionData(QueryOptions.defaultContextualOptions()).checkPermission(permission).asBoolean();
				}
			} else {
				return source.hasPermissionLevel(2); // Default to allow for ops if LuckPerms is not available
			}
		}
		return false;
	}

	private Text createMessage(String message, Formatting color) {
		return Text.literal(message).formatted(color);
	}

	private void ensureDirectoryExists() {
		File configDir = new File("config/smpspectator");
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
	}

	private void loadConfig() {
		try {
			if (CONFIG_FILE.exists()) {
				config.load(new FileReader(CONFIG_FILE));
				LOGGER.info("Config loaded successfully.");
			} else {
				saveConfig();
				LOGGER.info("Config file created and saved.");
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load config.", e);
		}
	}

	private void saveConfig() {
		try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
			config.save(writer);
			LOGGER.info("Config saved successfully.");
		} catch (IOException e) {
			LOGGER.error("Failed to save config.", e);
		}
	}

	private void loadData() {
		try {
			if (DATA_FILE.exists()) {
				playerDataMap.clear();
				Map<UUID, PlayerData> loadedData = PlayerData.load(new FileReader(DATA_FILE));
				if (loadedData != null) {
					playerDataMap.putAll(loadedData);
				}
				LOGGER.info("Player data loaded successfully.");
			} else {
				saveData(); // Create the file with default values if it doesn't exist
				LOGGER.info("Player data file created and saved.");
			}
		} catch (IOException e) {
			LOGGER.error("Failed to load player data.", e);
		}
	}

	private void saveData() {
		try (FileWriter writer = new FileWriter(DATA_FILE)) {
			PlayerData.save(writer, playerDataMap);
			LOGGER.info("Player data saved successfully.");
		} catch (IOException e) {
			LOGGER.error("Failed to save player data.", e);
		}
	}

	private int executeToggle(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player != null) {
			toggleSpectatorMode(player);
		}
		return 1;
	}

	private void toggleSpectatorMode(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		PlayerData data = playerDataMap.getOrDefault(playerId, new PlayerData(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.getWorld().getRegistryKey().getValue()));

		if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
			// Switch to Survival and restore position
			ServerWorld world = player.getServer().getWorld(player.getWorld().getRegistryKey());
			if (world != null) {
				player.teleport(world, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
				player.changeGameMode(GameMode.SURVIVAL);
				player.sendMessage(createMessage("You are now in SURVIVAL mode.", Formatting.GREEN), false);
				playerDataMap.remove(playerId);
			}
		} else {
			// Save position and switch to Spectator
			data.update(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.getWorld().getRegistryKey().getValue());
			playerDataMap.put(playerId, data);
			player.changeGameMode(GameMode.SPECTATOR);
			player.sendMessage(createMessage("You are now in SPECTATOR mode.", Formatting.AQUA), false);
		}

		saveData();
	}
}
