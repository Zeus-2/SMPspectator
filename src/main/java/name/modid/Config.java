package name.modid;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class Config {
    private final Properties configProperties;
    private final File configFile;
    boolean enabled = true;
    int maxSpeed = 10;
    private static final Logger LOGGER = Logger.getLogger("Config");

    public Config() {
        this.configProperties = new Properties();
        this.configFile = new File("config/smpspectator/config.conf");
        ensureConfigFileExists();
        load();
    }

    private void ensureConfigFileExists() {
        try {
            if (!configFile.getParentFile().exists()) {
                boolean dirCreated = configFile.getParentFile().mkdirs();
                LOGGER.info("Directory creation " + (dirCreated ? "successful" : "failed or unnecessary"));
            }
            if (!configFile.exists()) {
                boolean fileCreated = configFile.createNewFile();
                LOGGER.info("Config file creation " + (fileCreated ? "successful" : "failed or unnecessary"));
                save(); // Initialize with default values
            }
        } catch (IOException e) {
            LOGGER.severe("Failed to create config file: " + e.getMessage());
        }
    }

    public void load() {
        try (FileInputStream fis = new FileInputStream(configFile)) {
            configProperties.load(fis);
            this.enabled = Boolean.parseBoolean(configProperties.getProperty("enabled", "true"));
            this.maxSpeed = Integer.parseInt(configProperties.getProperty("maxSpeed", "10"));
        } catch (IOException e) {
            LOGGER.severe("Failed to load configuration: " + e.getMessage());
            // Defaults already set, could log warning about using default settings
        }
    }

    public void save() {
        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            configProperties.setProperty("enabled", Boolean.toString(enabled));
            configProperties.setProperty("maxSpeed", Integer.toString(maxSpeed));
            configProperties.store(fos, "SMPspectator Configuration");
        } catch (IOException e) {
            LOGGER.severe("Failed to save configuration: " + e.getMessage());
        }
    }
}
