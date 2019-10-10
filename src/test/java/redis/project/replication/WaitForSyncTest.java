package redis.project.replication;

import org.junit.Test;
import redis.clients.jedis.Jedis;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/10 08:27
 */
public class WaitForSyncTest {

    @Test
    public void isSync() {
        Jedis master = new Jedis("127.0.0.1");
        master.select(1);
        Jedis slave = new Jedis("127.0.0.1", 6378);
        slave.select(1);
        WaitForSync.isSync(master, slave);
    }
}