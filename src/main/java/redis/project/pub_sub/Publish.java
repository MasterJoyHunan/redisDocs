package redis.project.pub_sub;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

/**
 * 发布订阅系统
 */
public class Publish extends Thread {

    @Override
    public void run() {
        Jedis redis = RedisUtil.getRedis();
        try {
            sleep(1000);
            for (int i = 0; i < 5; i++) {
                redis.publish("channel", i + "");
                sleep(1000);
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) {
        new Publish().start();
        Jedis redis = RedisUtil.getRedis();
        redis.subscribe(new Sub(), "channel");
    }
}
