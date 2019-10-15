package redis.project.logs;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;
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


    @Test
    public void updateStats() {
        for (int i = 0; i < 5; i++) {
            int rand = new Random().nextInt(20);
            new Log().updateStats("page", rand);
        }
    }


    @Test
    public void getStats() {
        Map<String, Double> res = new Log().getStats("page");
        for (Map.Entry<String, Double> entry : res.entrySet()) {
            System.out.println(entry.getKey() + " => " + entry.getValue());
        }
    }
}