package redis.project.logs;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/15 17:16
 */
public class ConfigTest {

    @Test
    public void isUnderMaintenance() {
        for (int i = 0; i < 1000; i++) {
            System.out.println(new Config().isUnderMaintenance());
            try {
                Thread.sleep(new Random().nextInt(500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void setConfig() {
        new Config().setConfig("is_init", 1 + "");
    }


    @Test
    public void getConfig() {
        for (int i = 0; i < 10; i++) {
            System.out.println(new Config().getConfig("is_init"));
            try {
                Thread.sleep(new Random().nextInt(500));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}