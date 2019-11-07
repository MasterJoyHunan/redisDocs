package redis.project.shard;

import redis.clients.jedis.Jedis;
import redis.project.config.Config;

import java.util.HashMap;
import java.util.Map;

/**
 * @author joy
 * @time 2019/11/05 07:59
 */
public class Conn {

    /**
     * redis配置文件
     */
    public static final Map<String, Map<String, String>> REDIS_CONFIGS = new HashMap<>();

    /**
     * redis连接缓存
     */
    public static final Map<String, Jedis> REDIS_CONNECTS = new HashMap<>();


    /**
     * 在多master情况下，获取分片master实例
     *
     * @param key
     * @param member
     * @param shardCount
     * @return
     */
    public static Jedis getShardConnect(String key, String member, int shardCount) {
        String   shardId = Shard.shardKey(key, "x" + member, shardCount, 2);
        String[] shards  = shardId.split(":");
        return getRedisConn(shards[shards.length - 1]);
    }


    /**
     * 获取redis配置信息
     *
     * @param shard 分片ID
     * @return
     */
    public static Jedis getRedisConn(String shard) {

        String key = "CONFIG:REDIS:" + shard;
        Jedis  redis;
        redis = REDIS_CONNECTS.get(key);
        if (redis == null) {
            // 默认配置
            redis = new Jedis("localhost");
            redis.select(1);
            REDIS_CONNECTS.put(key, redis);
        }
        Map<String, String> oldConfig     = REDIS_CONFIGS.get(key);
        Map<String, String> currentConfig = Config.getConfig(redis, "REDIS", shard);

        // 如果新老配置不一致
        if (!compareMap(currentConfig, oldConfig)) {
            // 重新配置redis对象
            Jedis newRedis = new Jedis(currentConfig.get("host"), Integer.parseInt(currentConfig.get("port")));
            newRedis.select(Integer.parseInt(currentConfig.get("select")));
            if (currentConfig.get("auth") != null) {
                newRedis.auth(currentConfig.get("auth"));
            }
            REDIS_CONNECTS.put(key, newRedis);
            REDIS_CONFIGS.put(key, currentConfig);
        }
        return REDIS_CONNECTS.get(key);
    }


    /**
     * 对比两个Map是否一致
     *
     * @param map1
     * @param map2
     * @return
     */
    private static boolean compareMap(Map<String, String> map1, Map<String, String> map2) {
        if (map1 == null || map2 == null) {
            return false;
        }
        if (map1.size() != map2.size()) {
            return false;
        }
        for (Map.Entry<String, String> enter : map1.entrySet()) {
            String key   = enter.getKey();
            String value = map2.get(key);
            if (value == null) {
                return false;
            }
            if (!enter.getValue().equals(value)) {
                return false;
            }
        }
        return true;
    }
}
