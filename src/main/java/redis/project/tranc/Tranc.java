package redis.project.tranc;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import java.util.List;

/**
 * @author joy
 * @time 2019/10/10 18:56
 */
public class Tranc {

    /**
     * 上架商品至市场
     *
     * @param goodsId 商品ID
     * @param userId  用户ID
     * @param price   商品价格
     * @return
     */
    public boolean sellGoods(int goodsId, int userId, double price) {
        Jedis  redis      = RedisUtil.getRedis();
        String pack       = "pack:" + userId;
        String marketItem = userId + "|" + goodsId;
        // 500毫秒内有效
        Long deadline = System.currentTimeMillis() + 500;
        while (System.currentTimeMillis() < deadline) {
            redis.watch(pack);
            if (!redis.sismember(pack, goodsId + "")) {
                redis.unwatch();
                return false;
            }
            Transaction trans = redis.multi();
            // 上架
            trans.zadd("market:", price, marketItem);
            trans.srem(pack, goodsId + "");
            List<Object> res = trans.exec();
            if (res.size() == 0) {
                continue;
            }
            return true;
        }
        return false;
    }


    /**
     * 买家从市场购买商品
     *
     * @param buyerID    买家ID
     * @param sellUserId 卖家ID
     * @param goodsId    商品ID
     * @param oldPrice   商品没买之前的价格
     * @return
     */
    public boolean buyGoods(int buyerID, int sellUserId, int goodsId, double oldPrice) {
        String marketId = sellUserId + "|" + goodsId;
        String buyer    = "user:" + buyerID;
        String seller   = "user:" + sellUserId;
        String bPack    = "pack:" + buyerID;
        Jedis  redis    = RedisUtil.getRedis();
        long   deadline = System.currentTimeMillis() + 5000;
        while (System.currentTimeMillis() < deadline) {
            redis.watch("market:", buyer);
            Double price  = redis.zscore("market:", marketId);
            double wallet = Double.parseDouble(redis.hget(buyer, "wallet"));

            // 如果：1.东西被卖掉了 2.用户不存在 3.兜里钱不够 4.商品的价格出现变化 则回退
            if (price == null || wallet == 0 || wallet < price || oldPrice != price) {
                redis.unwatch();
                return false;
            }

            // 1.卖家扣钱 2.买家加钱 3.市场删除 4.卖家背包增加商品
            Transaction trans = redis.multi();
            trans.hincrByFloat(buyer, "wallet", -price);
            trans.hincrByFloat(seller, "wallet", price);
            trans.zrem("market:", marketId);
            trans.sadd(bPack, goodsId + "");
            List<Object> res = trans.exec();
            if (res.size() == 0) {
                continue;
            }
            return true;
        }
        return false;
    }
}
