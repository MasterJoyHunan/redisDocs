package redis.project.job;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/01 09:52
 */
public class NormalTest {

    @Test
    public void indexJob() {
        new Normal().indexJob(1, new String[]{"撑船", "打铁", "卖豆腐"});
        new Normal().indexJob(2, new String[]{"理发", "卖豆腐"});
        new Normal().indexJob(3, new String[]{"撑船", "理发"});
        new Normal().indexJob(4, new String[]{"撑船", "卖豆腐"});
        new Normal().indexJob(5, new String[]{"撑船", "打铁"});
        new Normal().indexJob(6, new String[]{"打铁", "卖豆腐"});
        new Normal().indexJob(7, new String[]{"撑船", "打铁"});
        new Normal().indexJob(8, new String[]{"撑船", "打铁", "卖豆腐"});
    }

    @Test
    public void findJobs() {
        System.out.println(new Normal().findJobs(new String[]{"撑船", "打铁"}));
    }
}