package redis.project.logs;


import redis.RedisUtil;
import redis.clients.jedis.Pipeline;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author joy
 * @time 2019/10/09 08:32
 */
public class Log {

    public void recent(String name, String msg, String level) {
        Pipeline         pipe   = RedisUtil.getRedis().pipelined();
        SimpleDateFormat time   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String           newMsg = "[" + time.format(new Date()) + "] [" +level + "] " + msg;
        String           key    = "LOG_RECENT:" + level + ":" + name;
        pipe.lpush(key, newMsg);
        pipe.ltrim(key, 0, 100);
        pipe.sync();
    }
}
