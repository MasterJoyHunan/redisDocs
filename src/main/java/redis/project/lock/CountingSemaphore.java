package redis.project.lock;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * 计数信号量
 *
 * @author joy
 * @time 2019/10/17 15:03
 */
public class CountingSemaphore {

    /**
     * 允许访问计数
     */
    public static final int LIMIT = 6;

    /**
     * 超时时间
     */
    public static final int TIME_OUT = 10000;

    /**
     * 获取计数信号锁
     *
     * @param lockName 锁名
     * @return
     */
    public static String acquireSemaphore(String lockName) {
        String uniqueId = UUID.randomUUID().toString();

        // 在多服务器情况下，不同服务器的时间有可能有差异
        long currentTime = System.currentTimeMillis();

        Jedis       redis   = RedisUtil.getRedis();
        Transaction trans   = redis.multi();
        String      lockKey = "SEMAPHORE:" + lockName;

        trans.zremrangeByScore(lockKey, 0, currentTime - TIME_OUT);
        trans.zadd(lockKey, currentTime, uniqueId);
        trans.zrank(lockKey, uniqueId);
        List<Object> res = trans.exec();
        if (res.size() == 0) {
            return null;
        }
        if ((long) res.get(res.size() - 1) >= LIMIT) {
            redis.zrem(lockKey, uniqueId);
            return null;
        }
        return uniqueId;
    }


    /**
     * 释放计数信号锁
     *
     * @param lockName 锁名
     * @param uniqueId 锁成员
     */
    public static void releaseSemaphore(String lockName, String uniqueId) {
        Jedis redis = RedisUtil.getRedis();
        redis.zrem("SEMAPHROE:" + lockName, uniqueId);
    }
}
