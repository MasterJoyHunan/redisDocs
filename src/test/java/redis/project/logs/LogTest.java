package redis.project.logs;

import org.junit.Test;

import java.util.Random;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/11 19:27
 */
public class LogTest {

    @Test
    public void recent() {
        for (int i = 0; i < 108; i++) {
            int random = new Random().nextInt(20);
            new Log().recent("page", "" + random, "info");
        }
    }

    @Test
    public void commonLog() {
        for (int i = 0; i < 108; i++) {
            int random = new Random().nextInt(20);
            new Log().commonLog("page", "" + random, "info");
        }
    }

    @Test
    public void updateCount() {
        new Log().updateCount("page");
        new Log().updateCount("aoe");
        new Log().updateCount("kill");
    }


    @Test
    public void getCount() {
        new Log().getCount("page", 300);
//        new Log().getCount("aoe", 300);
//        new Log().getCount("kill", 300);
    }

}