package redis;

import redis.clients.jedis.Jedis;

/**
 * @author joy
 * @time 2019/09/30 08:24
 */
public class RedisUtil {

    public static final Jedis redis;

    static {
        redis = new Jedis("localhost");
        redis.select(1);
    }

    public static Jedis getRedis() {
        return redis;
    }
}
