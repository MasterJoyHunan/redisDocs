package redis.project;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

/**
 * @author joy
 * @time 2019/09/30 16:37
 */
public class Cart {

    public void addToCart(String token, int pId, int count) {
        Jedis redis = RedisUtil.getRedis();
        if (count <= 0) {
            redis.hdel("user:cart:" + token, pId + "");
        } else {
            redis.hset("user:cart:" + token, pId + "", count + "");
        }
    }


     
}
