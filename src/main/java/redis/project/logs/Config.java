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
    private static int waitTime = 1000;

    private static


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
            Jedis  redis = RedisUtil.getRedis();
            String flag  = redis.get("CONFIG:is_under_maintenance");
            IS_UNDER_MAINTENANCE = "1".equals(flag);
        }
        return IS_UNDER_MAINTENANCE;
    }

    /**
     * 设置配置
     * @param keyName
     * @param config
     */
    public void setConfig(String keyName, String config) {
        Jedis redis = RedisUtil.getRedis();
        redis.set("CONFIG:" + keyName, config);
    }

    public Map<String, String> getConfig(String keyName) {
        int wait = 1000;
        String key = "CONFIG:" + keyName ;

        Long lastChecked = CHECKED.get(key);
        if (lastChecked == null || lastChecked < System.currentTimeMillis() - wait){
            CHECKED.put(key, System.currentTimeMillis());

            String value = conn.get(key);
            Map<String,Object> config = null;
            if (value != null){
                Gson gson = new Gson();
                config = (Map<String,Object>)gson.fromJson(
                        value, new TypeToken<Map<String,Object>>(){}.getType());
            }else{
                config = new HashMap<String,Object>();
            }

            CONFIGS.put(key, config);
        }

        return CONFIGS.get(key);
    }

}
