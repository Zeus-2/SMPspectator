package name.modid.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public class DatabaseManager {
    public static Connection connection;

    public static void initialize() {
        try {
            connection = DriverManager.getConnection("jdbc:h2:./config/smpspectator/database", "sa", "");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void savePlayerData(UUID uuid, PlayerData data) {
        String sql = "INSERT OR REPLACE INTO playerdata (uuid, x, y, z, yaw, pitch, world) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.setDouble(2, data.getX());
            stmt.setDouble(3, data.getY());
            stmt.setDouble(4, data.getZ());
            stmt.setFloat(5, data.getYaw());
            stmt.setFloat(6, data.getPitch());
            stmt.setString(7, data.getWorld());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void deletePlayerData(UUID uuid) {
        String sql = "DELETE FROM playerdata WHERE uuid = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, uuid.toString());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
