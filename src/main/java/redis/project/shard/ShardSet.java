package redis.project.shard;

import redis.RedisUtil;

/**
 * @author joy
 * @time 2019/11/04 13:56
 */
public class ShardSet {

    /**
     * 基于分片 写入set
     *
     * @param base         基础set名字
     * @param member       将要被储存到分片set里面的值
     * @param totalElement 预计元素总数量
     * @param shardSize    每个分片里包含的数量
     * @return
     */
    public Long shardAdd(String base, String member, int totalElement, int shardSize) {
        String shardId = Shard.shardKey(base, "s" + member, totalElement, shardSize);
        return RedisUtil.getRedis().sadd(shardId, member);
    }
}
