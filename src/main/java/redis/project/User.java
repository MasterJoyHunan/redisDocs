package redis.project;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * @author joy
 * @time 2019/09/30 14:11
 */
public class User {


    public void login(String username, String password) {
        String token = "";
        try {
            MessageDigest m = MessageDigest.getInstance("MD5");
            m.update((username + password).getBytes());
            token = m.getAlgorithm();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        int uid = new Random().nextInt(100);
        RedisUtil.getRedis().hset("user:login", token, uid + "");
    }

    /**
     * 判断是否登录状态
     *
     * @param token 用户token
     */
    public void checkToken(String token) {
        String user = RedisUtil.getRedis().hget("user:login", token);
        System.out.println(user);
    }

    /**
     * 浏览过的文章
     *
     * @param token 用户token
     * @param uid   用户id
     * @param aid   文章id
     */
    public void updateToken(String token, int uid, int aid) {
        Jedis redis = RedisUtil.getRedis();
        Long  time  = System.currentTimeMillis();
        // token映射UID
        redis.hset("user:login", token, uid + "");
        // 最后登录时间
        redis.zadd("user:last", time, token);
        // 加入浏览记录
        redis.zadd("user:view:" + token, time, aid + "");
        // 保留浏览记录最后25条
        redis.zremrangeByRank("user:view:" + token, 0, -26);
    }
}
