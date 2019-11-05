package redis.project.logs;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Map;

/**
 * 装饰器
 * @author joy
 * @time 2019/10/15 08:29
 */
public class AccessTime {

    private Jedis redis;
    private long  start;

    public AccessTime() {
        redis = RedisUtil.getRedis();
    }

    public void start() {
        start = System.currentTimeMillis();
    }

    public void stop(String context) {
        long delta = System.currentTimeMillis() - start;
        Log  log   = new Log();
        log.updateStats(context, delta / 1000.0);
        Map<String, Double> map = log.getStats(context);
        double              avg = map.get("avg") != null ? map.get("avg") : 0;
        if (map.get("count") > 100 && avg > map.get("sd") * 2) {
            log.recent(context, context + "渲染速度高于标准差" + map.get("sd") + "当前" + delta, "warning");
        }
        Transaction trans = redis.multi();
        trans.zadd("SLOWEST:access_time", avg, context);
        trans.zremrangeByRank("SLOWEST:access_time", 0, -101);
        trans.exec();
    }
}
