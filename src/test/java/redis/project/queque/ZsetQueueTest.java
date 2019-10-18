package redis.project.queque;

import org.junit.Test;

import java.util.Random;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/18 13:55
 */
public class ZsetQueueTest {

    @Test
    public void laterQueue() {
        for (int i = 0; i < 10; i++) {
            ZsetQueue.laterQueue(
                    "crontab",
                    UUID.randomUUID().toString(),
                    new Random().nextInt(30000));
        }
    }

    @Test
    public void run() {
        new Thread(() -> {
            for (int i = 0; i < 100; i++) {
                System.out.println(i);
            }
        }).start();
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}