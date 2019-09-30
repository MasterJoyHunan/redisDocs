package redis;

import redis.clients.jedis.Jedis;

/**
 * @author joy
 * @time 2019/09/30 08:24
 */
public class RedisUtil {

    public static Jedis getRedis() {
        Jedis redis = new Jedis("localhost");
        redis.select(1);
        return redis;
    }
}
