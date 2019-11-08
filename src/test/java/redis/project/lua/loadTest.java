package redis.project.lua;

import org.junit.Test;

/**
 * @author joy
 * @time 2019/11/07 20:07
 */
public class loadTest {

    @Test
    public void scriptLoad() {
        System.out.println(new LuaLoad().scriptLoad("return 1"));
    }


}