package name.modid;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.command.ServerCommandSource;

public class PermissionHandler {

    public boolean hasPermission(ServerCommandSource source, String permissionNode) {
        return Permissions.check(source, permissionNode, 2); // The '2' is the default op-level fallback if no permissions system is in place.
    }
}
