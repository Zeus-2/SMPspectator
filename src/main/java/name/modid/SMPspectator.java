package name.modid;

import name.modid.commands.SMPspectatorCommands;
import name.modid.config.ConfigManager;
import name.modid.data.PlayerDataManager;
import name.modid.permissions.PermissionHandler;
import name.modid.utils.ConditionChecker;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Formatting;
import net.minecraft.world.GameMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraft.text.Text;

public class SMPspectator implements ModInitializer {
	private static final Logger LOGGER = LogManager.getLogger(SMPspectator.class);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing SMPspectator...");

		// Ensure the directory exists and load configuration
		ConfigManager.ensureDirectoryExists();
		ConfigManager.loadConfig();
		LOGGER.info("Initializing configuration...");

		// Load and save player data
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			PlayerDataManager.loadFromFile();
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				PlayerDataManager.loadPlayerData(player, server);
			}
			LOGGER.info("Loaded player data...");
		});

		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server is stopping. Saving data...");
			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				PlayerDataManager.savePlayerData(player);
			}
			PlayerDataManager.saveToFile();
			LOGGER.info("Saved player data...");
		});

		// Register commands and permission handler
		SMPspectatorCommands.registerCommands();
		LOGGER.info("Initializing registered commands...");

		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			try {
				LuckPerms luckPerms = LuckPermsProvider.get();
				PermissionHandler.init(luckPerms);
				LOGGER.info("Initializing permission handler...");
			} catch (Exception e) {
				LOGGER.error("Failed to initialize LuckPerms: " + e.getMessage());
			}
		});

		LOGGER.info("SMPspectator initialized successfully.");
	}



	public static void toggleSpectatorMode(ServerPlayerEntity player) {
		if (player == null) return;

		if (player.interactionManager.getGameMode() == GameMode.SPECTATOR) {
			PlayerDataManager.loadPlayerData(player, player.getServer());
			player.changeGameMode(GameMode.SURVIVAL);
			player.clearStatusEffects(); // Clears all effects when returning to Survival mode
			player.sendMessage(Text.literal("You are now in Survival mode.").formatted(Formatting.GREEN));
		} else {
			if (ConditionChecker.canEnterSpectator(player)) {
				PlayerDataManager.savePlayerData(player); // Save current data before switching
				PlayerDataManager.saveToFile(); // Immediately write to file
				player.changeGameMode(GameMode.SPECTATOR);
				player.sendMessage(Text.literal("You are now in Spectator mode.").formatted(Formatting.DARK_AQUA));
			}
		}
	}
}



