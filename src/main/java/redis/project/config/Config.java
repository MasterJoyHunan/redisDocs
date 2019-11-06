package redis.project.config;


import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**
 * 服务配置与发现
 *
 * @author joy
 * @time 2019/11/05 08:33
 */
public class Config {

    /**
     * 所有配置信息
     */
    private static final Map<String, Map<String, String>> CONFIGS = new HashMap<>();

    /**
     * 所有配置信息最后检查时间
     */
    private static final Map<String, Long> CHECKED = new HashMap<>();

    /**
     * 每N秒检查一次
     */
    private static final int WAIT = 1000;


    /**
     * 获取配置文件
     *
     * @param type      配置类型
     * @param component 组件
     * @return
     */
    public static Map<String, String> getConfig(Jedis conn, String type, String component) {
        String key       = "CONFIG:" + type + ":" + component;
        Long   lastCheck = CHECKED.get(key);
        if (lastCheck == null || lastCheck < System.currentTimeMillis() - WAIT) {
            Map<String, String> config = conn.hgetAll(key);
            CHECKED.put(key, System.currentTimeMillis());
            CONFIGS.put(key, config);
        }
        return CONFIGS.get(key);
    }


    /**
     * 设置配置文件
     *
     * @param type      配置类型
     * @param component 组件
     * @param config    配置详情
     */
    public static void setConfig(Jedis conn, String type, String component, Map<String, String> config) {
        conn.hmset("CONFIG:" + type + ':' + component, config);
    }
}
