package redis.project.shard;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/08 14:55
 */
public class ShardListTest {

    @Test
    public void shardLpush() {
        new ShardList().shardLpush("TEST_LIST", new String[] {"q", "w", "e", "r", "t"});
    }

    @Test
    public void shardRpush() {
        new ShardList().shardRpush("TEST_LIST", new String[] {"a", "b", "c", "d", "e"});
    }
}