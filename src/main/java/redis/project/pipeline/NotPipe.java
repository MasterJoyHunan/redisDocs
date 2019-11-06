package redis.project.pipeline;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

/**
 * @author joy
 * @time 2019/10/08 09:04
 */
public class NotPipe extends Thread {


    @Override
    public void run() {
        Jedis    redis    = RedisUtil.getRedis();
        System.out.println("incr :" + redis.incr("incr"));
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        redis.decr("incr");

    }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new NotPipe().start();
        }
    }
}
