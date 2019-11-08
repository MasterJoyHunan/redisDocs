package redis.project.lua;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author joy
 * @time 2019/11/07 19:55
 */
public class LuaLoad {

    public final static Map<String, String> SHA1_SCRIPT = new HashMap<>();

    public static Object scriptLoad(String script, List<String> keys, List<String> args) {
        Jedis redis = RedisUtil.getRedis();
        if (!SHA1_SCRIPT.containsKey(script)) {
            String sha1 = RedisUtil.getRedis().scriptLoad(script);
            SHA1_SCRIPT.put(script, sha1);
        }
        try {
            return redis.evalsha(SHA1_SCRIPT.get(script), keys, args);
        } catch (Exception e) {
            return redis.eval(script, keys, args);
        }
    }

    public static Object scriptLoad(String script, int keyCount, String... params) {
        Jedis redis = RedisUtil.getRedis();
        if (!SHA1_SCRIPT.containsKey(script)) {
            String sha1 = RedisUtil.getRedis().scriptLoad(script);
            SHA1_SCRIPT.put(script, sha1);
        }
        try {
            return redis.evalsha(SHA1_SCRIPT.get(script), keyCount, params);
        } catch (Exception e) {
            return redis.eval(script, keyCount, params);
        }
    }

    public static Object scriptLoad(String script) {
        Jedis redis = RedisUtil.getRedis();
        if (!SHA1_SCRIPT.containsKey(script)) {
            String sha1 = RedisUtil.getRedis().scriptLoad(script);
            SHA1_SCRIPT.put(script, sha1);
        }
        try {
            return redis.evalsha(SHA1_SCRIPT.get(script));
        } catch (Exception e) {
            return redis.eval(script);
        }
    }
}
