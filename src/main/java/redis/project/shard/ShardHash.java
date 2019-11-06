package redis.project.shard;

import redis.RedisUtil;

/**
 * @author joy
 * @time 2019/11/04 13:50
 */
public class ShardHash {

    /**
     * 基于分片 写入hash
     *
     * @param base         基础hash名字
     * @param key          将要被储存到分片hash里面的键
     * @param value        将要被储存到分片hash里面的值
     * @param totalElement 预计元素总数量
     * @param shardSize    每个分片里包含的数量
     * @return
     */
    public Long shardHset(String base, String key, String value, int totalElement, int shardSize) {
        String shardId = Shard.shardKey(base, key, totalElement, shardSize);
        return RedisUtil.getRedis().hset(shardId, key, value);
    }


    /**
     * 基于分片 读取hash
     *
     * @param base         基础hash名字
     * @param key          将要被读取到分片hash里面的键
     * @param totalElement 预计元素总数量
     * @param shardSize    每个分片里包含的数量
     * @return
     */
    public String shardHget(String base, String key, int totalElement, int shardSize) {
        String shardId = Shard.shardKey(base, key, totalElement, shardSize);
        System.out.println(shardId);
        return RedisUtil.getRedis().hget(shardId, key);
    }
}
