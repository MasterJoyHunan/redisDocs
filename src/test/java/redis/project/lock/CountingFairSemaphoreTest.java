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


    @Test
    public void refreshSemaphore() {
        CountingFairSemaphore.refreshSemaphore("test", "85a05d2b-8c4d-4985-958b-4ae41eb9ef21");
    }

    @Test
    public void acquireSemaphoreWithLock() {
        for (int i = 0; i < 1000; i++) {
            CountingFairSemaphore.acquireSemaphoreWithLock("qqqccc");
        }
    }

}