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
import net.minecraft.world.GameMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class SMPspectator implements ModInitializer {
	private static final Logger LOGGER = LogManager.getLogger(SMPspectator.class);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing SMPspectator...");

		// Ensure the directory exists and load configuration
		ConfigManager.ensureDirectoryExists();
		ConfigManager.loadConfig();
		LOGGER.info("Initializing configuration...");

		// Load player data
		PlayerDataManager.loadData();
		LOGGER.info("Initializing playerData...");

		// Register to save data on server stop
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			LOGGER.info("Server is stopping. Saving data...");
			PlayerDataManager.saveData();
		});

		// Register commands
		SMPspectatorCommands.registerCommands();
		LOGGER.info("Initializing registered commands...");

		// Initialize permission handler after the server has started
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
			PlayerDataManager.returnToSurvival(player);
		} else if (ConditionChecker.canEnterSpectator(player)) {
			PlayerDataManager.enterSpectator(player);
		}
	}
}
