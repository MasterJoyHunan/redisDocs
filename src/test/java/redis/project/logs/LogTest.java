package redis.project.logs;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/11 19:27
 */
public class LogTest {

    @Test
    public void recent() {
        for (int i = 0; i < 108; i++) {
            new Log().recent("page", "xxx" + Math.random(), "info");
        }
    }
}