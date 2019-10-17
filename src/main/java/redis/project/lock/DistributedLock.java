package redis.project.lock;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.UUID;

/**
 * 分布式锁 V1
 *
 * @author joy
 * @time 2019/10/17 08:37
 */
public class DistributedLock {

    public static final int TIME_OUT = 10;

    /**
     * 获取锁（加锁）
     *
     * @return
     */
    public static String acquireLock(String lockName) {
        String uniqueId = UUID.randomUUID().toString();
        long   deadline = System.currentTimeMillis() + TIME_OUT;
        Jedis  redis    = RedisUtil.getRedis();
        while (System.currentTimeMillis() < deadline) {
            if (redis.setnx("LOCK:" + lockName, uniqueId) != 0) {
                return uniqueId;
            }
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**
     * 释放锁
     *
     * @param lockName 锁名
     * @param uniqueId 对应的值
     */
    public static void releaseLock(String lockName, String uniqueId) {
        Jedis  redis        = RedisUtil.getRedis();
        String fullLockName = "LOCK:" + lockName;
        while (true) {
            redis.watch(lockName);
            if (!uniqueId.equals(redis.get(fullLockName))) {
                redis.unwatch();
                return;
            }
            Transaction trans = redis.multi();
            trans.del(fullLockName);
            List<Object> res = trans.exec();
            if (res.size() == 0) {
                continue;
            }
            return;
        }
    }

}
