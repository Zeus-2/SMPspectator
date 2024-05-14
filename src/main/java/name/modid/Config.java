package name.modid;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.TypeDescription;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.Reader;
import java.io.Writer;

public class Config {
    public boolean enabled = true;
    public int maxSpeed = 10;

    private static final Yaml yaml;

    static {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setIndent(2);
        dumperOptions.setPrettyFlow(true);
        dumperOptions.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);

        Representer representer = new Representer(dumperOptions);
        representer.addClassTag(Config.class, new Tag("!config"));

        Constructor constructor = new Constructor(new LoaderOptions());
        TypeDescription configDesc = new TypeDescription(Config.class, new Tag("!config"));
        constructor.addTypeDescription(configDesc);

        yaml = new Yaml(constructor, representer);
    }

    public void load(Reader reader) {
        Config loadedConfig = yaml.load(reader);
        if (loadedConfig != null) {
            this.enabled = loadedConfig.enabled;
            this.maxSpeed = loadedConfig.maxSpeed;
        }
    }

    public void save(Writer writer) {
        yaml.dump(this, writer);
    }
}
