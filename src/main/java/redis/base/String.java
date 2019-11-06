package redis.base;

import redis.RedisUtil;

/**
 * @author joy
 * @time 2019/09/30 08:26
 */
public class String {

    public void get() {
        java.lang.String string = RedisUtil.getRedis().get("hello");
        System.out.println(string);
    }

    public void set() {
        RedisUtil.getRedis().set("hello", "world");
    }

    public void del() {
        RedisUtil.getRedis().del("hello");
        this.get();
    }
}
