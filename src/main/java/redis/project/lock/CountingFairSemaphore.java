package redis.project.lock;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;

import java.util.List;
import java.util.UUID;

/**
 * 公平信号量锁
 *
 * @author joy
 * @time 2019/10/17 16:55
 */
public class CountingFairSemaphore {

    /**
     * 允许访问计数
     */
    public static final int LIMIT = 6;

    /**
     * 超时时间
     */
    public static final int TIME_OUT = 10000;

    /**
     * 获取公平信号量锁
     *
     * @param lockName 锁名
     * @return
     */
    public static String acquireSemaphore(String lockName) {
        String uniqueId     = UUID.randomUUID().toString();
        String lockOwnerKey = "SEMAPHORE:" + lockName + ":owner";
        String lockKey      = "SEMAPHORE:" + lockName;
        String countKey     = "SEMAPHORE_COUNT:" + lockName;
        long   currentTime  = System.currentTimeMillis();
        Jedis  redis        = RedisUtil.getRedis();

        Transaction trans = redis.multi();
        trans.zremrangeByScore(lockKey, 0, currentTime - TIME_OUT);

        // 将 (lockOwnerKey 里的 score * 1)  + ( lockKey 里的 score * 0) 的结果写入 lockOwnerKey
        trans.zinterstore(lockOwnerKey, new ZParams().weightsByDouble(1, 0), lockOwnerKey, lockKey);
        // 自增计数器
        trans.incr(countKey);
        List<Object> results = trans.exec();
        int counter = ((Long)results.get(results.size() - 1)).intValue();

        trans = redis.multi();
        trans.zadd(lockKey, currentTime, uniqueId);
        trans.zadd(lockOwnerKey, counter, uniqueId);
        trans.zrank(lockOwnerKey, uniqueId);
        results = trans.exec();
        int result = ((Long)results.get(results.size() - 1)).intValue();
        if (result < LIMIT){
            return uniqueId;
        }

        trans = redis.multi();
        trans.zrem(lockKey, uniqueId);
        trans.zrem(lockOwnerKey, uniqueId);
        trans.exec();
        return null;
    }
}
