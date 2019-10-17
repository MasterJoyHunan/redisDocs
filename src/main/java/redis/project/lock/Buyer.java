package redis.project.lock;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * @author joy
 * @time 2019/10/17 09:08
 */
public class Buyer {

    /**
     * 购买商品
     *
     * @param buyerId  购买人的ID
     * @param itemId   商品ID
     * @param sellerId 出售人ID
     * @return
     */
    public boolean purchaseItem(int buyerId, int itemId, int sellerId) {
        String buyerKey   = "user:" + buyerId;
        String buyerPack  = "pack:" + buyerId;
        String sellerKey  = "user:" + sellerId;
        String marketItem = sellerId + "|" + itemId;
        String markerKey  = "market:";
        Jedis  redis      = RedisUtil.getRedis();

        // 获取锁失败
        // String uniqueId = DistributedLock.acquireLock(markerKey);
        // String uniqueId = DistributedLockV2.acquireLock(markerKey);
        String uniqueId = CountingSemaphore.acquireSemaphore(markerKey);
        if (uniqueId == null) {
            return false;
        }
        while (true) {
            try {
                Double marketPrice = redis.zscore(markerKey, marketItem);

                // Double.parseDouble 可能会有异常
                Double hasPrice = Double.parseDouble(redis.hget(buyerKey, "wallet"));

                // 防止查不到的情况
                if (marketPrice == null || hasPrice - marketPrice < 0) {
                    return false;
                }
                Transaction trans = redis.multi();
                trans.hincrByFloat(buyerKey, "wallet", -marketPrice);
                trans.hincrByFloat(sellerKey, "wallet", marketPrice);
                trans.zrem(markerKey, marketItem);
                trans.sadd(buyerPack, itemId + "");
                List<Object> res = trans.exec();
                if (res.size() == 0) {
                    continue;
                }
                return true;
            } catch (NullPointerException e) {
                return false;
            } finally {
                // 不管怎么样 都要解锁
                // DistributedLock.releaseLock(markerKey, uniqueId);
                CountingSemaphore.releaseSemaphore(markerKey, uniqueId);
            }
        }
    }
}
