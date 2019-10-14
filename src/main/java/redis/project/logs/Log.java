package redis.project.logs;


import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @author joy
 * @time 2019/10/09 08:32
 */
public class Log {

    public static final int[] TIME_COUNT = {1, 5, 60, 300, 3600, 18000, 86400};

    /**
     * 记录最新出现的日志
     *
     * @param name  日志类型
     * @param msg   日志详情
     * @param level 日志等级
     */
    public void recent(String name, String msg, String level) {
        Pipeline         pipe   = RedisUtil.getRedis().pipelined();
        SimpleDateFormat time   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String           newMsg = "[" + time.format(new Date()) + "] " + msg;
        String           key    = "LOG_RECENT:" + name + ":" + level;
        pipe.lpush(key, newMsg);
        pipe.ltrim(key, 0, 100);
        pipe.sync();
    }


    /**
     * 记录最新和最常出现的日志
     *
     * @param name  日志类型
     * @param msg   日志详情
     * @param level 日志等级
     */
    public void commonLog(String name, String msg, String level) {
        String key      = "LOG_COMMON:" + name + ":" + level;
        String startKey = key + ":start";
        long   end      = System.currentTimeMillis() + 5;
        Jedis  redis    = RedisUtil.getRedis();
        String newMsg   = "[" + new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()) + "] " + msg;
        while (System.currentTimeMillis() < end) {
            redis.watch(startKey);
            String      hourStart = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            String      existing  = redis.get(startKey);
            Transaction trans     = redis.multi();
            existing = existing == null ? "" : existing;

            //
            if (hourStart.compareTo(existing) > 0) {
                // 上次记录的数据
                trans.rename(key + ":record", key + ":last");
                // 上次记录的时间
                trans.rename(startKey, key + ":pre_start");
                // 记录最新的时间
                trans.set(startKey, hourStart);
            }
            trans.zincrby(key + ":count", 1, msg);

            trans.lpush(key + ":record", newMsg);
            trans.ltrim(key + ":record", 0, 100);

            List<Object> res = trans.exec();
            if (res.size() == 0) {
                continue;
            }
            return;
        }
    }


    /**
     * 页面计数
     *
     * @param name 页面
     */
    public void updateCount(String name) {

        Jedis       redis = RedisUtil.getRedis();
        Transaction trans = redis.multi();
        long        now   = System.currentTimeMillis() / 1000;
        for (int i : TIME_COUNT) {
            long   pnow = (int) (now / i) * i;
            String hash = i + ":" + name;
            trans.zadd("LOG:log_tag:", 0, hash);
            trans.hincrBy(
                    "LOG:count:" + hash,
                    new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(pnow * 1000)),
                    1);
        }
        trans.exec();
    }


    /**
     *
     *
     * @param name 页面
     * @param precision 时间进度
     */
    public void getCount(String name, int precision) {
        String hash = precision + ":" + name;
        Jedis  redis = RedisUtil.getRedis();
        Map<String, String> res = redis.hgetAll("LOG:count:" + hash);
        for (Map.Entry<String, String> entry : res.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }
}
