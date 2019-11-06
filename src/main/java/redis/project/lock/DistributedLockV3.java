package redis.project.lock;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.Collections;
import java.util.UUID;

/**
 * @author joy
 * @time 2019/10/21 08:33
 */
public class DistributedLockV3 {

    /**
     * 分布式锁V2的优化版
     * 保证设置锁和设置过期时间的原子性
     *
     * @param lockName 锁名
     * @return
     */
    public static String acquireLock(String lockName) {
        lockName = "LOCK:" + lockName;
        Jedis  redis    = RedisUtil.getRedis();
        String uniqueId = UUID.randomUUID().toString();
        String res      = redis.set(lockName, uniqueId, "NX", "PX", 200);
        if ("OK".equals(res)) {
            return uniqueId;
        }
        return null;
    }

    /**
     * 解锁
     *
     * @param lockName 锁名
     * @param uniqueId 唯一标识
     */
    public static void releaseLock(String lockName, String uniqueId) {
        lockName = "LOCK:" + lockName;
        Jedis redis = RedisUtil.getRedis();
        String script =
                "if redis.call('get', KEYS[1]) == ARGV[1]  then " +
                "return redis.call('del', KEYS[1]) " +
                "else " +
                "return 0 " +
                "end";
        redis.eval(script, Collections.singletonList(lockName), Collections.singletonList(uniqueId));
    }
}
