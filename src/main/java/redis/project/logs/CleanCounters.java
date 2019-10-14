package redis.project.logs;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.Set;

/**
 * @author joy
 * @time 2019/10/14 14:25
 */
public class CleanCounters extends Thread {

    private long timeOffset;

    public CleanCounters(long timeOffset) {
        this.timeOffset = timeOffset;
    }

    @Override
    public void run() {
        Jedis redis = RedisUtil.getRedis();
        while (true) {
            long start = System.currentTimeMillis() + timeOffset;
            int  index = 0;
            while (index < redis.zcard("LOG:log_tag:")) {
                Set<String> key = redis.zrange("LOG:log_tag:", index, index);
                index++;
                if (key.size() == 0) {
                    break;
                }
                String   hash      = key.iterator().next();
                String[] keys      = hash.split(":");
                int      percision = Integer.parseInt(keys[0]);
                int bprec = (int)Math.floor(percision / 60);
                if (bprec == 0){
                    bprec = 1;
                }
                if ((percision % bprec) != 0){
                    continue;
                }
            }
        }
    }
}
