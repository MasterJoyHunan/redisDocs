package redis.project.ziplist;

import redis.RedisUtil;
import redis.clients.jedis.DebugParams;

/**
 * 压缩
 *
 * @author joy
 * @time 2019/11/04 08:58
 */
public class zip {

    public static void main(String[] args) {
        System.out.println(new zip().index("CITY_ID_TO_CITY:"));
    }

    public String index(String key) {
        return RedisUtil.getRedis().debug(DebugParams.OBJECT(key));
    }
}
