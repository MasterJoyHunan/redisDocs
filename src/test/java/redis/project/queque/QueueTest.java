package redis.project.queque;

import org.junit.Test;
import redis.project.Const;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/18 09:57
 */
public class QueueTest {

    @Test
    public void sendEmailViaQueue() {
        for (int i = 0; i < 100; i++) {
            new Queue().sendEmailViaQueue(Const.UID, 3, i + ": hello world");
        }

    }

    @Test
    public void run() {
        new Queue().start();
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}