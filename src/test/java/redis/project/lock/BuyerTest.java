package redis.project.lock;

import org.junit.Test;
import redis.RedisUtil;
import redis.project.Const;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/17 10:46
 */
public class BuyerTest {

    @Test
    public void purchaseItem() {
        RedisUtil.getRedis().zadd("market:", 2, "9527|3");
        new Buyer().purchaseItem(3, 3, Const.UID);
    }


}