package redis.project.bit;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/04 17:37
 */
public class CodeTest {

    @Test
    public void getCode() {
        System.out.println(new Code().getCode("USA", "AA"));
    }


    @Test
    public void setLocation() {
        new Code().setLocation(28,"USA", "AE");
    }
}