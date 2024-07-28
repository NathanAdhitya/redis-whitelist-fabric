package name.rediswhitelist;

import com.mojang.authlib.GameProfile;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.CommandManager;
import net.minecraft.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RedisWhitelist implements ModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("redis-whitelist");
    public static JedisPool jedisPool;
    private final Path configPath;
    private final JedisPoolConfig jedisPoolConfig;

    public RedisWhitelist() {
        super();

        configPath = FabricLoader.getInstance().getConfigDir().resolve("frw.toml");

        jedisPoolConfig = new JedisPoolConfig();
        jedisPoolConfig.setMaxTotal(64);
        jedisPoolConfig.setMaxIdle(2);
        jedisPoolConfig.setMinIdle(1);
        jedisPoolConfig.setTestWhileIdle(true);
    }



    @Override
    public void onInitialize() {
        // Register reload command
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(CommandManager.literal("frw")
                .then(CommandManager.literal("reload")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(context -> {
                        reloadConfig();
                        context.getSource().sendFeedback(() -> Text.literal("Config reloaded!"), false);
                        return 1;
                    })));
        });

        reloadConfig();

        LOGGER.info("Loaded Fabric Redis Whitelist");
    }

    private void reloadConfig() {
        Config config;
        try {
            Files.createDirectories(configPath.getParent());
            config = Config.read(configPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Replace the Jedis pool with a new one
        try {
            JedisPool newPool = new JedisPool(jedisPoolConfig, config.getRedisUri());
            JedisPool oldPool = jedisPool;
            jedisPool = newPool;
            if (oldPool != null && !oldPool.isClosed()) {
                oldPool.close();
            }
        } catch (Exception e) {
            LOGGER.error("An error occurred while creating a new Jedis pool: ", e);
        }
    }
}