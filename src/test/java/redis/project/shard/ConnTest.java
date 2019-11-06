package redis.project.shard;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/05 13:49
 */
public class ConnTest {

    @Test
    public void getShardConnect() {

        String base = "TEST:HASH:";
        String key = "A";
        Jedis redis;
        redis = Conn.getShardConnect(base, key, 2);
        redis.hset(base, key, "1");

        key = "D";
        redis = Conn.getShardConnect(base, key, 2);
        redis.hset(base, key, "2");
    }
}