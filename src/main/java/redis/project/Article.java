package redis.project;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * @author joy
 * @time 2019/09/30 09:01
 */
public class Article {

    /**
     * 添加文章入缓存
     * 并添加文章入排序
     *
     * @param aid 文章ID
     */
    public void add(int aid) {
        Map map = new HashMap();
        map.put("id", aid + "");
        map.put("title", "hello world");
        map.put("time", System.currentTimeMillis() + "");
        map.put("vote", "0");
        Jedis redis = RedisUtil.getRedis();
        redis.hmset("article:" + aid, map);
        redis.zadd("score:article", 0, Integer.toString(aid));
    }

    /**
     * 分页显示
     *
     * @param page 当前页码
     */
    public void getPage(String key, int page) {
        page = page == 0 ? 1 : page;
        int         pageSize   = 3;
        int         start      = (page - 1) * pageSize;
        int         end        = start + pageSize - 1;
        Jedis       redis      = RedisUtil.getRedis();
        Set<String> articleIds = redis.zrevrange(key, start, end);
        for (String aid : articleIds) {
            Map<String, String> article = redis.hgetAll("article:" + aid);
            System.out.println(article);
        }
        System.out.println("===");
    }

    /**
     * 加入对应的群组
     *
     * @param aid    文章ID
     * @param groups 群组
     */
    public void addToGroup(int aid, String[] groups) {
        Jedis redis = RedisUtil.getRedis();
        for (String group : groups) {
            redis.sadd("article_group:" + group, aid + "");
        }
    }


    /**
     * 从对应的群组里删除
     *
     * @param aid    文章ID
     * @param groups 群组
     */
    public void removeToGroup(int aid, String[] groups) {
        Jedis redis = RedisUtil.getRedis();
        for (String group : groups) {
            redis.srem("article_group:" + group, aid + "");
        }
    }


    /**
     * 获取分类排行
     *
     * @param group 群组
     * @param page 分页
     */
    public void getGroup(String group, int page) {
        Jedis  redis = RedisUtil.getRedis();
        String key   = "score:group:" + group;
        if (!redis.exists(key)) {
            redis.zinterstore(key, "article_group:" + group, "score:article");
            redis.expire(key, 60);
        }
        getPage(key, page);
    }
}
