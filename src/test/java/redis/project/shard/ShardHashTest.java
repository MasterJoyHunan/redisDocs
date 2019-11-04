package redis.project.shard;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.ScanParams;
import redis.clients.jedis.ScanResult;

import java.util.Map;

/**
 * @author joy
 * @time 2019/11/04 13:51
 */
public class ShardHashTest {

    public final static int TOTAL_ELEMENT = 900000;
    public final static int SHARD_SIZE    = 500;

    @Test
    public void shardHset() {
        ScanResult<Map.Entry<String, String>> res;

        String     next       = "0";
        ScanParams scanParams = new ScanParams().count(1000);
        ShardHash  shard      = new ShardHash();
        do {
            res = RedisUtil.getRedis().hscan("CITY_ID_TO_CITY:", next, scanParams);
            for (Map.Entry<String, String> item : res.getResult()) {
                shard.shardHset("CITY_ID_TO_CITY", item.getKey(), item.getValue(), TOTAL_ELEMENT, SHARD_SIZE);
            }
            next = res.getStringCursor();
        } while (!"0".equals(next));
    }

    @Test
    public void shardHget() {
        System.out.println(new ShardHash().shardHget("CITY_ID_TO_CITY", "5073", TOTAL_ELEMENT, SHARD_SIZE));
    }
}