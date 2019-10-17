package redis.project.lock;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import java.util.UUID;

/**
 * 分布式锁V2 附带超时时间
 *
 * @author joy
 * @time 2019/10/17 11:15
 */
public class DistributedLockV2 {
    /**
     * 加锁时间 --毫秒
     */
    public static final int ACQUIRE_TIME_OUT = 10;

    /**
     * 锁自动释放时间 --秒
     */
    public static final int LOCK_TIME_OUT = 10;


    public static String acquireLock(String lockName) {
        String uniqueId = UUID.randomUUID().toString();
        long   deadline = System.currentTimeMillis() + ACQUIRE_TIME_OUT;
        String lockKey  = "LOCK:" + lockName;
        Jedis  redis    = RedisUtil.getRedis();
        while (System.currentTimeMillis() < deadline) {
            if (redis.setnx(lockKey, uniqueId) != 0) {
                redis.expire(lockKey, LOCK_TIME_OUT);
                return uniqueId;
            } else {
                // 防止在执行 setnx 和 expire 之间 redis 服务器如果挂掉
                // 有可能会出现 锁未设置过期时间，则为其设置过期时间
                if (redis.ttl(lockKey) != 0) {
                    redis.expire(lockKey, LOCK_TIME_OUT);
                }
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

}
