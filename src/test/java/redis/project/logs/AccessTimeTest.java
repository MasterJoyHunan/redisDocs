package redis.project.logs;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/15 08:58
 */
public class AccessTimeTest {

    @Test
    public void tests() {
        AccessTime accessTime = new AccessTime();
        for (int i = 0; i < 120; i++) {
            accessTime.start();

            try {
                Thread.sleep(new Random().nextInt(1000));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            accessTime.stop("page_index");
        }
    }
}