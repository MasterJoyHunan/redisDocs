package redis.project.logs;


import redis.RedisUtil;
import redis.clients.jedis.*;

import java.text.SimpleDateFormat;
import java.util.*;

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
     * @param name      页面
     * @param precision 时间进度
     */
    public void getCount(String name, int precision) {
        String              hash  = precision + ":" + name;
        Jedis               redis = RedisUtil.getRedis();
        Map<String, String> res   = redis.hgetAll("LOG:count:" + hash);
        for (Map.Entry<String, String> entry : res.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }


    /**
     * 记录某个上下文的最大值，最小值，总和，开方，数量
     *
     * @param name  上下文
     * @param value 数值
     */
    public void updateStats(String name, double value) {
        String destination = "STATS:" + name;
        String startTime   = destination + ":start";
        Jedis  redis       = RedisUtil.getRedis();
        long   deadline    = System.currentTimeMillis() + 500;
        while (System.currentTimeMillis() < deadline) {
            redis.watch(startTime);
            long        currentTime   = System.currentTimeMillis();
            String      lastWrite     = redis.get(startTime);
            long        lastWriteTime = lastWrite == null ? 0 : Long.parseLong(lastWrite);
            Transaction trans         = redis.multi();

            // 如果当前时间 大于上次写入时间的N豪秒
            long offset = 1000 * 60 * 60;
            if (currentTime - lastWriteTime >= offset) {
                trans.rename(destination + ":record", destination + ":last");
                trans.rename(destination + ":list", destination + ":pre_list");
                trans.rename(startTime, destination + ":pre_start");
                trans.set(startTime, currentTime + "");
            }

            // 生成临时zset
            String templateKey1 = UUID.randomUUID().toString();
            String templateKey2 = UUID.randomUUID().toString();
            trans.zadd(templateKey1, value, "min");
            trans.zadd(templateKey2, value, "max");

            // 查并集
            trans.zunionstore(destination + ":record",
                    new ZParams().aggregate(ZParams.Aggregate.MIN),
                    destination + ":record", templateKey1);
            trans.zunionstore(destination + ":record",
                    new ZParams().aggregate(ZParams.Aggregate.MAX),
                    destination + ":record", templateKey2);

            // 一系列自增操作
            trans.del(templateKey1, templateKey2);
            trans.zincrby(destination + ":record", 1, "count");
            trans.zincrby(destination + ":record", value, "sum");
            trans.zincrby(destination + ":record", value * value, "sumsq");
            trans.hset(destination + ":list", System.currentTimeMillis() + "", value + "");
            List<Object> res = trans.exec();
            if (res.size() == 0) {
                continue;
            }
            return;
        }
    }


    /**
     * 获取某个上下文的最大值，最小值，总和，开方，数量，平均值，标准差
     *
     * @param name 上下文
     * @return map
     */
    public Map<String, Double> getStats(String name) {
        String     key   = "STATS:" + name + ":record";
        Jedis      redis = RedisUtil.getRedis();
        Set<Tuple> res   = redis.zrangeWithScores(key, 0, -1);
        if (res.size() == 0) {
            return null;
        }

        // 将set转换为map
        Map<String, Double> resMap = new HashMap<>();
        for (Tuple tu : res) {
            resMap.put(tu.getElement(), tu.getScore());
        }
        double count = resMap.get("count");

        // 平均值
        double avg = resMap.get("sum") / count;
        resMap.put("avg", avg);

        // 标准差
        double numerator = resMap.get("sumsq") - Math.pow(resMap.get("sum"), 2) / count;
        double sd        = Math.pow(numerator / (count > 1 ? count - 1 : 1), 0.5);
        resMap.put("sd", sd);

        return resMap;
    }
}
