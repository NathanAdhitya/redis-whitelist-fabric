package name.rediswhitelist;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import com.google.gson.annotations.Expose;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;

public class Config {
    @Expose
    private final String redisUri;
    public Config(String redisUri) {
        this.redisUri = redisUri;
    }

    public static Config read(Path path) {
        URL defaultConfigLocation = Config.class.getClassLoader().getResource("frw.toml");

        if (defaultConfigLocation == null) {
            throw new RuntimeException("Default config not found!");
        }

        CommentedFileConfig config = CommentedFileConfig.builder(path)
            .defaultData(defaultConfigLocation)
            .autosave()
            .preserveInsertionOrder()
            .sync()
            .build();

        config.load();

        String redisUri = config.getOrElse("redisUri", "redis://localhost:6379");

        return new Config(redisUri);
    }

    public String getRedisUri() {
        return redisUri;
    }

}


