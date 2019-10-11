package redis.project.tranc;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.project.Const;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/10 19:22
 */
public class TrancTest {


    @Test
    public void shelves() {
        Jedis redis = RedisUtil.getRedis();
        // 创建背包

        for (int i = 1; i <= 10; i++) {
            redis.sadd("pack:" + Const.UID, i + "");
        }
        new Tranc().sellGoods(2, Const.UID, new Random().nextInt(10000));
    }


    @Test
    public void buyGoods() {
        Jedis redis    = RedisUtil.getRedis();
        int   buyerUid = 3;

        // 买家
        Map<String, String> buyerMap = new HashMap<>();
        buyerMap.put("id", buyerUid + "");
        buyerMap.put("name", "bill");
        buyerMap.put("wallet", 100 + "");
        redis.hmset("user:" + buyerUid, buyerMap);

        // 买家
        Map<String, String> sellerMap = new HashMap<>();
        sellerMap.put("id", Const.UID + "");
        sellerMap.put("name", "joy");
        sellerMap.put("wallet", 0 + "");
        redis.hmset("user:" + Const.UID, sellerMap);


        int    goodsId  = new Random().nextInt(500);
        double oldPrice = new Random().nextInt(20);
        double offset   = System.currentTimeMillis() % 2;
        redis.zadd("market:", oldPrice + offset, Const.UID + "|" + goodsId);
        redis.zadd("market:", oldPrice, Const.UID + "|" + goodsId);
        Tranc tranc = new Tranc();
        tranc.buyGoods(buyerUid, Const.UID, goodsId, oldPrice);
        tranc.buyGoods(buyerUid, Const.UID, goodsId, oldPrice);
        tranc.buyGoods(buyerUid, Const.UID, goodsId, oldPrice);
        tranc.buyGoods(buyerUid, Const.UID, goodsId, oldPrice);
    }
}