package redis.project.shard;

import lombok.AllArgsConstructor;
import lombok.Data;
import redis.clients.jedis.Jedis;

/**
 * @author joy
 * @time 2019/11/07 19:17
 */
@AllArgsConstructor
@Data
public class KeyShardConn {

    private String key;
    private int    shards;

    public Jedis getConn(String member) {
        return Conn.getShardConnect(key, member, shards);
    }
}
