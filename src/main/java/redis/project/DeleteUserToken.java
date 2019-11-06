package redis.project;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.Set;

/**
 * @author joy
 * @time 2019/09/30 15:00
 */
public class DeleteUserToken implements Runnable {
    public static void main(String[] args) {
        new Thread(new DeleteUserToken()).start();
    }

    private volatile boolean quit = false;


    @Override

    public void run() {
        Jedis redis = RedisUtil.getRedis();
        while (!quit) {
            Long size = redis.zcard("user:last");
            if (size <= Const.LIMIT) {
                System.out.println(size);
                System.out.println(System.currentTimeMillis());
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                continue;
            }
            long endIndex = Math.min(size - Const.LIMIT, 100);

            System.out.println(endIndex - 1);
            Set<String> tokenSet = redis.zrange("user:last", 0, endIndex - 1);
            for (String token : tokenSet) {
                System.out.println("rm some thing");
                redis.del("user:view:" + token);
                redis.hdel("user:login", token);
                redis.zrem("user:last", token);
                redis.zrem("user:cart", token);
            }
        }
    }

}
