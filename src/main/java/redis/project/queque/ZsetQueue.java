package redis.project.queque;

import com.google.gson.Gson;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;
import redis.project.lock.DistributedLock;
import redis.project.lock.DistributedLockV2;
import redis.project.logs.Log;

import java.util.*;

/**
 * 延迟队列
 *
 * @author joy
 * @time 2019/10/18 10:47
 */
public class ZsetQueue extends Thread {

    public static void main(String[] args) {
        new ZsetQueue().start();
    }

    /**
     * 加入延迟队列
     *
     * @param queue 队列名
     * @param data  数据
     * @param delay 延迟时间
     * @return
     */
    public static String laterQueue(String queue, String data, int delay) {
        String id    = UUID.randomUUID().toString();
        Jedis  redis = RedisUtil.getRedis();
        redis.zadd("DELAYED_QUEUE:",
                System.currentTimeMillis() + delay,
                new Gson().toJson(new String[]{id, queue, data}));
        return id;
    }


    public volatile boolean quit = false;

    /**
     * 定时完毕
     */
    @Override
    public void run() {
        Jedis redis = RedisUtil.getRedis();
        while (!quit) {
            Set<Tuple> res = redis.zrangeWithScores("DELAYED_QUEUE:", 0, 0);
            Tuple      top = res.size() > 0 ? res.iterator().next() : null;

            // 如果最小的score比当前时间还要大的话，说明任务还不需要开始
            // 休息一会继续工作
            if (top == null || top.getScore() > System.currentTimeMillis()) {
                try {
                    Thread.sleep(20);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                continue;
            }

            // 获取延时队列的信息
            String   data = top.getElement();
            String[] info = new Gson().fromJson(data, String[].class);
            String   id   = info[0];

            // 队列转移加锁
            String lock = DistributedLockV2.acquireLock(id);
            if (lock == null) {
                continue;
            }

            // [事务] 将延时任务加入到任务列队
            Transaction trans = redis.multi();
            trans.zrem("DELAYED_QUEUE:", data);
            trans.rpush("QUEUE:" + info[1], info[2]);
            List<Object> exec = trans.exec();
            if (exec.size() == 0) {
                new Log().commonLog("DELAYED_QUEUE", "执行异常", "error");
            }
            DistributedLock.releaseLock(id, lock);
        }
    }
}
