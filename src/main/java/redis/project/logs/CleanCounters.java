package redis.project.logs;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * @author joy
 * @time 2019/10/14 14:25
 */
public class CleanCounters extends Thread {

//    private long timeOffset = 86400000;

/*    public CleanCounters(long timeOffset) {
        this.timeOffset = timeOffset;
    }*/

    @Override
    public void run() {
        Jedis redis  = RedisUtil.getRedis();
        int   passes = 0;
        while (true) {
            long start = System.currentTimeMillis() + 86400000;
            int  index = 0;
            // 判断是否有数据
            while (index < redis.zcard("LOG:log_tag:")) {
                Set<String> key = redis.zrange("LOG:log_tag:", index, index);
                index++;
                if (key.size() == 0) {
                    break;
                }
                // 找出精度样本 precision = 1 、 5、 60、。。。。
                String   hash      = key.iterator().next();
                String[] keys      = hash.split(":");
                int      precision = Integer.parseInt(keys[0]);

                // 小于60秒的 bprec = 1
                int bprec = (int) Math.floor(precision / 60);
                if (bprec == 0) {
                    bprec = 1;
                }
                if ((passes % bprec) != 0) {
                    continue;
                }

                // 找到详情里记录
                String hkey = "LOG:count:" + hash;
                String cutoff = String.valueOf(
                        ((System.currentTimeMillis() + 86400000) / 1000) - 100 * precision);
                ArrayList<String> samples = new ArrayList<>(redis.hkeys(hkey));
                Collections.sort(samples);
                int remove = bisectRight(samples, cutoff);

                if (remove != 0) {
                    redis.hdel(hkey, samples.subList(0, remove).toArray(new String[0]));
                    if (remove == samples.size()) {
                        redis.watch(hkey);
                        if (redis.hlen(hkey) == 0) {
                            Transaction trans = redis.multi();
                            trans.zrem("known:", hash);
                            trans.exec();
                            index--;
                        } else {
                            redis.unwatch();
                        }
                    }
                }
            }
            passes++;
            long duration = Math.min(
                    (System.currentTimeMillis() + 86400000) - start + 1000, 60000);
            try {
                sleep(Math.max(60000 - duration, 1000));
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // mimic python's bisect.bisect_right
    public int bisectRight(List<String> values, String key) {
        int index = Collections.binarySearch(values, key);
        return index < 0 ? Math.abs(index) - 1 : index + 1;
    }



}
