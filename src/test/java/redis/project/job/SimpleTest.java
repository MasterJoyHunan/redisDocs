package redis.project.job;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/01 09:00
 */
public class SimpleTest {

    @Test
    public void addJob() {
        new Simple().addJob(1, new String[] {"a", "b"});
    }

    @Test
    public void findJob() {
        System.out.println(new Simple().findJob(1, new String[] {"a"}));
        System.out.println(new Simple().findJob(1, new String[] {"c"}));
        System.out.println(new Simple().findJob(1, new String[] {"a", "b", "c"}));
    }
}
