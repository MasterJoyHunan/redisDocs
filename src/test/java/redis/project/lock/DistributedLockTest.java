package redis.project.lock;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/18 09:01
 */
public class DistributedLockTest {

    @Test
    public void acquireLock() {
        System.out.println(DistributedLock.acquireLock("qqqccc"));
    }
}