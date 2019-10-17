package redis.project.lock;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;

import static org.junit.Assert.*;

public class CountingFairSemaphoreTest {

    @Test
    public void acquireSemaphore() {
        for (int i = 0; i < 7; i++) {
            String id = CountingFairSemaphore.acquireSemaphore("test");
            System.out.println(id);
        }
    }

    @Test
    public void releaseSemaphore() {
        CountingFairSemaphore.releaseSemaphore("test", "903be5d1-7f9f-42d6-a0bb-c8a345d5e318");
    }

}