package redis.project.pipeline;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * 管道
 * @author joy
 * @time 2019/10/09 08:25
 */
public class Pipe extends Thread {


    @Override
    public void run() {
        Jedis    redis    = RedisUtil.getRedis();
        Pipeline pipeline = redis.pipelined();
        pipeline.multi();
        System.out.println("incr :" + pipeline.incr("incr"));
        try {
            sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        pipeline.decr("incr");
        pipeline.exec();
    }

    public static void main(String[] args) {
        for (int i = 0; i < 3; i++) {
            new NotPipe().start();
        }
    }
}