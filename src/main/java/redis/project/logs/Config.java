package redis.project.logs;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务的发现与配置
 *
 * @author joy
 * @time 2019/10/15 17:07
 */
public class Config {
    /**
     * 最后确认时间
     */
    private static long lastCheckTime;

    /**
     * 是否是维护
     */
    private static boolean IS_UNDER_MAINTENANCE = false;

    /**
     * 等待一秒时间
     */
    private static final int WAIT_TIME = 1000;

    /**
     * 配置缓存
     */
    private static final Map<String, String> CONFIGS = new HashMap<>();

    /**
     * 何时获取过配置
     */
    private static final Map<String, Long> CHECKED = new HashMap<>();


    /**
     * 判断系统是否在维护中
     *
     * @return boolean
     */
    public boolean isUnderMaintenance() {
        long nowTime = System.currentTimeMillis();
        if (lastCheckTime < nowTime - 1000) {
            System.out.println(nowTime);
            lastCheckTime = nowTime;
            Jedis redis = RedisUtil.getRedis();
            String flag = redis.get("CONFIG:is_under_maintenance");
            IS_UNDER_MAINTENANCE = "1".equals(flag);
        }
        return IS_UNDER_MAINTENANCE;
    }

    /**
     * 设置配置
     *
     * @param keyName
     * @param config
     */
    public void setConfig(String keyName, String config) {
        Jedis redis = RedisUtil.getRedis();
        redis.set("CONFIG:" + keyName, config);
    }

    public String getConfig(String keyName) {
        String key = "CONFIG:" + keyName;
        Long currentTime = System.currentTimeMillis();
        Long lastChecked = CHECKED.get(key);
        Jedis redis = RedisUtil.getRedis();

        if (lastChecked == null || lastChecked < currentTime - WAIT_TIME) {
            // 将最后获取该配置的时间写入
            CHECKED.put(key, currentTime);

            // 获取配置 并将配置写入全局
            CONFIGS.put(key, redis.get(key));
        }
        return CONFIGS.get(key);
    }

}
