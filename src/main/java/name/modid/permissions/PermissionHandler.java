package name.modid.permissions;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.context.ContextManager;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class PermissionHandler {
    private static LuckPerms luckPerms;

    // Method to initialize LuckPerms
    public static void init(LuckPerms api) {
        luckPerms = api;
    }

    public static boolean hasPermission(ServerCommandSource source, String permission) {
        if (luckPerms == null) {
            throw new IllegalStateException("LuckPerms API is not initialized!");
        }

        // Get the player from the command source
        ServerPlayerEntity player = source.getPlayer();

        if (player == null) {
            return false; // Return false if the source is not a player
        }

        // Get the LuckPerms User
        User user = luckPerms.getUserManager().getUser(player.getUuid());

        if (user == null) {
            return false; // Return false if the user is not found
        }

        // Get the context manager and query options
        ContextManager contextManager = luckPerms.getContextManager();
        QueryOptions queryOptions = contextManager.getQueryOptions(user).orElse(contextManager.getStaticQueryOptions());

        // Check the permission
        return user.getCachedData().getPermissionData(queryOptions).checkPermission(permission).asBoolean();
    }
}
