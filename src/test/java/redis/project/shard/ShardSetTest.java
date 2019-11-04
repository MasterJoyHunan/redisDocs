package redis.project.shard;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.text.SimpleDateFormat;
import java.util.*;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/04 14:00
 */
public class ShardSetTest {
    public final static int SHARD_SIZE = 500;

    @Test
    public void shardAdd() {
        String uuid = UUID.randomUUID().toString().replace("-", "").substring(0, 15);
        Long   uid  = Long.parseLong(uuid, 16);

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
        String time = simpleDateFormat.format(new Date());

        Long isUnique = new ShardSet().shardAdd("WEBSITE:UNIQUE_" + time, String.valueOf(uid), SHARD_SIZE, SHARD_SIZE);
        if (isUnique > 0) {
            RedisUtil.getRedis().incr("WEBSITE:UNIQUE_" + time + ":");
        }
    }


    private Map<String, Long> EXPECTED = new HashMap<>();

    public long getExpected(String key) {
        Jedis conn = RedisUtil.getRedis();
        if (!EXPECTED.containsKey(key)) {
            String exkey       = key + ":expected";
            String expectedStr = conn.get(exkey);
            long   expected    = 0;
            if (expectedStr == null) {
                Calendar yesterday = Calendar.getInstance();
                yesterday.add(Calendar.DATE, -1);

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
                simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
                String time = simpleDateFormat.format(yesterday.getTime());

                expectedStr = conn.get("WEBSITE:UNIQUE_:" + time);
                expected = expectedStr != null ? Long.parseLong(expectedStr) : 1000000;

                expected = (long) Math.pow(2, (long) (Math.ceil(Math.log(expected * 1.5) / Math.log(2))));
                if (conn.setnx(exkey, String.valueOf(expected)) == 0) {
                    expectedStr = conn.get(exkey);
                    expected = Integer.parseInt(expectedStr);
                }
            } else {
                expected = Long.parseLong(expectedStr);
            }
            EXPECTED.put(key, expected);
        }
        return EXPECTED.get(key);
    }

}