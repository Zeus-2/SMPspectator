package name.modid.config;

import java.io.*;
import java.util.Properties;

public class ConfigManager {
    public static final Config config = new Config();

    public static void ensureDirectoryExists() {
        File configDir = new File("config/smpspectator");
        if (!configDir.exists()) {
            configDir.mkdirs();
        }
    }

    public static void saveConfig() {
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

    public static void loadConfig() {
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
}
