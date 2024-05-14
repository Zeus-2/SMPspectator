package name.modid;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.representer.Representer;

public class YamlUtils {
    private static final Yaml yaml;

    static {
        // LoaderOptions setup
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);

        // Setup DumperOptions
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        // Create Representer
        Representer representer = new Representer(dumperOptions);

        // Create Constructor
        Constructor constructor = new Constructor(loaderOptions);
        constructor.addTypeDescription(new TypeDescription(PlayerData.class, "!PlayerData"));

        yaml = new Yaml(constructor, representer);
    }

    public static Yaml getYaml() {
        return yaml;
    }
}
