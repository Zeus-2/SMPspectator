package name.modid;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.LeadItem;
import net.minecraft.item.TridentItem;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.GameMode;
import net.minecraft.world.World;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;

public class SMPspectator implements ModInitializer {
	private static final File DATA_FILE = new File("config/smpspectator/data.json");
	private static final Config config = new Config();
	private static final Map<UUID, PlayerData> playerDataMap = new HashMap<>();
	private final PermissionHandler permissionHandler = new PermissionHandler();
	private static final Logger LOGGER = LogManager.getLogger(SMPspectator.class);


	public static class PatternTypeAdapter extends TypeAdapter<Pattern> {
		@Override
		public void write(JsonWriter out, Pattern value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value.pattern());
			}
		}

		@Override
		public Pattern read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			} else {
				return Pattern.compile(in.nextString());
			}
		}
	}

	public static class TimeZoneTypeAdapter extends TypeAdapter<TimeZone> {
		@Override
		public void write(JsonWriter out, TimeZone value) throws IOException {
			if (value == null) {
				out.nullValue();
			} else {
				out.value(value.getID());
			}
		}

		@Override
		public TimeZone read(JsonReader in) throws IOException {
			if (in.peek() == JsonToken.NULL) {
				in.nextNull();
				return null;
			} else {
				return TimeZone.getTimeZone(in.nextString());
			}
		}
	}

	private Gson createGson() {
		return new GsonBuilder()
				.registerTypeAdapter(Pattern.class, new PatternTypeAdapter())
				.registerTypeAdapter(TimeZone.class, new TimeZoneTypeAdapter())
				.setPrettyPrinting()
				.create();
	}

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing SMPspectator...");

		// Ensure the directory exists
		LOGGER.info("Ensuring configuration directory exists...");
		ensureDirectoryExists();

		// Load the configuration
		LOGGER.info("Loading configuration...");
		loadConfig(); // Load configuration first

		// Load player data
		LOGGER.info("Loading player data...");
		loadData();

		// Register to save data on server stop
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server is stopping. Saving data...");
			saveData();
		});

		// Register the player position handler
		LOGGER.info("Loading Position handler...");
		PlayerPositionHandler.register();

		// Register commands
		LOGGER.info("Registering commands...");
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			registerCommands(dispatcher);
		});

		LOGGER.info("SMPspectator initialized successfully.");
	}



	private void refreshCommandsForAllPlayers(ServerCommandSource source) {
		MinecraftServer server = source.getServer();
		server.getPlayerManager().getPlayerList().forEach(
				player -> server.getCommandManager().sendCommandTree(player)
		);
	}


	private void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		dispatcher.register(CommandManager.literal("s")
				.executes(context -> {
					ServerCommandSource source = context.getSource();
					ServerPlayerEntity player = source.getPlayer();
					if (config.enabled) {
                        assert player != null;
                        toggleSpectatorMode(player);
						// Optionally send feedback about the toggle
					} else {
						// Send a colored error message
						source.sendMessage(Text.literal("Spectator mode toggling is currently disabled.").formatted(Formatting.RED));
					}
					return 1;
				})
				.then(CommandManager.literal("force")
						.requires(src -> config.enabled && permissionHandler.hasPermission(src, "smpspectator.force"))
						.then(CommandManager.argument("target", EntityArgumentType.player())
								.executes(context -> {
									ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "target");
									toggleSpectatorMode(target);
									return 1;
								})
						)
				)
				.then(CommandManager.literal("enable")
						.requires(src -> !config.enabled && permissionHandler.hasPermission(src, "smpspectator.enable"))
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							ServerPlayerEntity player = source.getPlayer();
							config.enabled = true;
							saveConfig();
							refreshCommandsForAllPlayers(context.getSource());
							source.sendMessage(Text.literal("Spectator toggle is now enabled").formatted(Formatting.GREEN));
							return 1;
						})
				)
				.then(CommandManager.literal("disable")
						.requires(src -> config.enabled && permissionHandler.hasPermission(src, "smpspectator.disable"))
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							ServerPlayerEntity player = source.getPlayer();
							config.enabled = false;
							saveConfig();
							refreshCommandsForAllPlayers(context.getSource());
							source.sendMessage(Text.literal("Spectator toggle is now disable").formatted(Formatting.RED));
							return 1;
						})
				)
				.then(CommandManager.literal("reload")
						.requires(src -> permissionHandler.hasPermission(src, "smpspectator.reload"))
						.executes(context -> {
							ServerCommandSource source = context.getSource();
							ServerPlayerEntity player = source.getPlayer();
							loadConfig();
							refreshCommandsForAllPlayers(context.getSource());
							source.sendMessage(Text.literal("Config reloaded").formatted(Formatting.LIGHT_PURPLE));
							return 1;
						})
				)
				.then(CommandManager.literal("speed")
						.requires(src -> config.enabled && permissionHandler.hasPermission(src, "SMPspectator.speed"))
						.then(CommandManager.argument("speed", IntegerArgumentType.integer(1, config.maxSpeed))
								.executes(context -> {
									ServerPlayerEntity player = context.getSource().getPlayer();
									assert player != null;
									if (player.interactionManager.getGameMode() != GameMode.SPECTATOR) {
										context.getSource().sendError(Text.literal("This command is only available in Spectator mode."));
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







	private Text createMessage(String message, Formatting color) {
		return Text.literal(message).formatted(color);
	}

	private void ensureDirectoryExists() {
		File configDir = new File("config/smpspectator");
		if (!configDir.exists()) {
			configDir.mkdirs();
		}
	}

	private void saveConfig() {
		Properties properties = new Properties();
		properties.setProperty("enabled", String.valueOf(config.enabled));
		properties.setProperty("maxSpeed", String.valueOf(config.maxSpeed));

		try (FileOutputStream fos = new FileOutputStream("config/smpspectator/config.conf")) {
			properties.store(fos, "SMPspectator Configuration");
		} catch (IOException e) {
			System.err.println("Failed to save configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private void loadConfig() {
		Properties properties = new Properties();
		try (FileInputStream fis = new FileInputStream("config/smpspectator/config.conf")) {
			properties.load(fis);
			config.enabled = Boolean.parseBoolean(properties.getProperty("enabled", "true"));
			config.maxSpeed = Integer.parseInt(properties.getProperty("maxSpeed", "10"));
		} catch (IOException e) {
			System.err.println("Failed to load configuration: " + e.getMessage());
			e.printStackTrace();
		}
	}


	private void loadData() {
		if (DATA_FILE.exists()) {
			try (Reader reader = new FileReader(DATA_FILE)) {
				Gson gson = createGson();
				Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
				Map<UUID, PlayerData> loadedData = gson.fromJson(reader, type);
				playerDataMap.clear();
				if (loadedData != null) {
					playerDataMap.putAll(loadedData);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void saveData() {
		try (Writer writer = new FileWriter(DATA_FILE)) {
			Gson gson = createGson();
			Type type = new TypeToken<Map<UUID, PlayerData>>() {}.getType();
			gson.toJson(playerDataMap, type, writer);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}





	// Helper method to check if the player is near a portal
	private boolean isNearPortal(ServerPlayerEntity player) {
		BlockPos pos = player.getBlockPos();
		World world = player.getWorld();
		int range = 2; // Check within a 2-block radius; adjust as necessary.

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



	// Modified toggleSpectatorMode method to include portal proximity check
	private void toggleSpectatorMode(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		PlayerData data = playerDataMap.getOrDefault(playerId, new PlayerData(
				player.getX(),
				player.getY(),
				player.getZ(),
				player.getYaw(),
				player.getPitch(),
				player.getWorld().getRegistryKey()
		));

		if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
			// Retrieve the target world based on saved data
			ServerWorld targetWorld = Objects.requireNonNull(player.getServer()).getWorld(data.getWorld());
			if (targetWorld != null) {
				player.teleport(targetWorld, data.getX(), data.getY(), data.getZ(), data.getYaw(), data.getPitch());
				player.changeGameMode(GameMode.SURVIVAL);
				playerDataMap.remove(playerId);
				saveData();
				player.sendMessage(createMessage("You are now in SURVIVAL mode.", Formatting.GREEN), false);
				LOGGER.info(player.getName().getString() + " is now in SURVIVAL mode.");
			}
		} else {
			if (!player.isOnGround()) {
				player.sendMessage(Text.literal("You must be touching the ground to switch to spectator mode.").formatted(Formatting.RED));
				return;
			}
			if (!conditionsToEnterSpectator(player)) {
				return;
			}
			data.update(player.getX(), player.getY(), player.getZ(), player.getYaw(), player.getPitch(), player.getWorld().getRegistryKey());
			playerDataMap.put(playerId, data);
			player.changeGameMode(GameMode.SPECTATOR);
			saveData();
			player.sendMessage(createMessage("You are now in SPECTATOR mode.", Formatting.AQUA), false);
			LOGGER.info(player.getName().getString() + " is now in SPECTATOR mode.");
		}
	}

	private boolean conditionsToEnterSpectator(ServerPlayerEntity player) {
		// Example condition: Check if the player is holding restricted items
		boolean isHoldingRestrictedItem = player.getMainHandStack().getItem() instanceof BowItem ||
				player.getMainHandStack().getItem() instanceof CrossbowItem ||
				player.getMainHandStack().getItem() instanceof TridentItem ||
				player.getMainHandStack().getItem() instanceof LeadItem;

		if (isHoldingRestrictedItem) {
			player.sendMessage(Text.literal("You cannot switch to spectator mode while holding restricted items.").formatted(Formatting.RED));
			return false;
		}

		// Example condition: Check for negative status effects
		boolean hasNegativeEffect = player.hasStatusEffect(StatusEffects.POISON) ||
				player.hasStatusEffect(StatusEffects.WEAKNESS) ||
				player.hasStatusEffect(StatusEffects.SLOWNESS);

		if (hasNegativeEffect) {
			player.sendMessage(Text.literal("You cannot switch to spectator mode while affected by negative status effects.").formatted(Formatting.RED));
			return false;
		}

		// Add more conditions as needed
		return true;
	}


	private void setPlayerSpeed(ServerPlayerEntity player, int speed) {
		if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
			// Calculate the new fly speed based on some base speed. Minecraft's default is 0.05f.
			// Let's assume 'speed' is a multiplier where 1 is normal speed, 2 is twice the speed, etc.
			float baseSpeed = 0.05f;
			float newSpeed = baseSpeed * speed;

			// Set the new fly speed
			player.getAbilities().setFlySpeed(newSpeed);
			player.sendAbilitiesUpdate();

			// Notify the player
			player.sendMessage(Text.literal("Spectator fly speed set to " + speed + "."), false);
		} else {
			player.sendMessage(Text.literal("This command is only available in Spectator mode."), false);
		}
	}

}
