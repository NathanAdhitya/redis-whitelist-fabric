package name.rediswhitelist.mixin;

import com.mojang.authlib.GameProfile;
import name.rediswhitelist.RedisWhitelist;
import net.minecraft.server.PlayerManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

@Mixin(PlayerManager.class)
public class PlayerManagerMixin {

    @Redirect(
        method = "checkCanJoin",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/server/PlayerManager;isWhitelisted(Lcom/mojang/authlib/GameProfile;)Z"
        )
    )
    public boolean isWhitelisted(PlayerManager instance, GameProfile profile) {
        Jedis jedis = null;
        try {
            // Get Jedis Pool
            JedisPool jedisPool = RedisWhitelist.jedisPool;
            jedis = jedisPool.getResource();

            boolean isPlayerWhitelisted = jedis.sismember("whitelist.main", profile.getId().toString());
            if (!isPlayerWhitelisted) {
                RedisWhitelist.LOGGER.info("Player {} with UUID {} is not whitelisted", profile.getName(),
                    profile.getId()
                );

                return false;
            }
            RedisWhitelist.LOGGER.info("Player {} with UUID {} is whitelisted", profile.getName(),
                profile.getId()
            );
            return true;
        } catch (Exception e) {
            RedisWhitelist.LOGGER.error("An error occurred while checking if player {} with UUID {} is whitelisted",
                profile.getName(), profile.getId()
            );
            return false;
        } finally {
            try {
                if (jedis != null) {
                    jedis.close();
                }
            } catch (Exception e) {
                RedisWhitelist.LOGGER.error("An error occurred while closing Jedis connection: ", e);
            }
        }
    }
}
