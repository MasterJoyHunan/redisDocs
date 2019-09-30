package redis.base;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/09/30 08:30
 */
public class StringTest {
    String string = new String();

    @Test
    public void set() {
        string.set();
    }

    @Test
    public void get() {
         string.get();
    }



    @Test
    public void del() {
        string.del();
    }
}