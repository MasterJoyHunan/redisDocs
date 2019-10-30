package redis.project.ad;

import redis.RedisUtil;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;
import redis.project.search.InvertedIndexes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * 广告平台
 *
 * @author joy
 * @time 2019/10/30 08:15
 */
public class AdvertisementPlatform {

    /**
     * 创建广告索引
     *
     * @param id        广告ID
     * @param locations 广告地理位置
     * @param content   广告关键词
     * @param type      广告投放计费属性
     * @param cost      单次计费
     */
    public void advertisementIndex(String id, String[] locations, String content, String type, double cost) {
        Transaction     trans = RedisUtil.getRedis().multi();
        InvertedIndexes index = new InvertedIndexes();

        // 将位置信息存储
        for (String location : locations) {
            trans.sadd("AD:LOCATION:" + location, id);
        }

        // 反向索引
        Set<String> words = index.tokenize(content);
        for (String word : words) {
            trans.sadd("AD:INDEX:" + word, id);
            trans.sadd("AD:WORD:" + id, word);
        }
        // 求平均值
        double ecpm = toECPM(type, 1000., 1., cost);
        // 各种记录
        trans.hset("AD:TYPES:", id, type);
        trans.zadd("AD:ECPM:", ecpm, id);
        trans.zadd("AD:BASE_COST", cost, id);
        trans.exec();
    }


    /**
     * 广告定向操作
     *
     * @param locations 地区
     * @param content   关键词
     * @return
     */
    public String targetAds(String[] locations, String content) {
        String      id    = matchLocation(locations);
        Transaction trans = RedisUtil.getRedis().multi();

    }


    /**
     * 估算广告在1000次展示获取的收益
     *
     * @param type  按查看次数收费、点击次数收费、购买次数收费
     * @param views 查看多少次
     * @param avg   通过率 (点击次数/查看次数) (购买次数/点击次数)
     * @param cost  查看次数收费价格、点击次数收费价格、购买次数收费价格
     * @return
     */
    public double toECPM(String type, double views, double avg, double cost) {
        switch (type) {
            case "cpa":
            case "cpc":
                return 1000. * cost * avg / views;
            case "cpv":
            default:
                return cost;
        }
    }


    /**
     * 按地区匹配广告
     *
     * @param locations 地区
     * @return
     */
    public String matchLocation(String[] locations) {
        // 交集
        String[] items = new String[locations.length];
        for (int i = 0; i < locations.length; i++) {
            items[i] = "AD:LOCATION:" + locations[i];
        }
        String id = new InvertedIndexes().union(items);
        return id;
    }


    /**
     * 计算包含了内容匹配的附加值广告
     *
     * @param matched 根据地理位置匹配成功的广告
     * @param base
     * @param content 附加参数（关键词）
     */
    public void finishScoring(String matched, String base, String content) {
        Map<String, Integer> bonusEcpm = new HashMap<>();
        InvertedIndexes      ind       = new InvertedIndexes();
        Set<String>          words     = ind.tokenize(content);
        for (String word : words) {
            String wordBonus = ind.zintersect(new ZParams().weightsByDouble(0, 1), matched, word);
            bonusEcpm.put(wordBonus, 1);
        }

        if (bonusEcpm.size() > 0) {
            String[] keys    = new String[bonusEcpm.size()];
            int[]    weights = new int[bonusEcpm.size()];
            int      index   = 0;
            for (Map.Entry<String, Integer> bonus : bonusEcpm.entrySet()) {
                keys[index] = bonus.getKey();
                weights[index] = bonus.getValue();
                index++;
            }

            ZParams minParams = new ZParams().aggregate(ZParams.Aggregate.MIN).weights(weights);
            String  minimum   = ind.zunion(minParams, keys);

            ZParams maxParams = new ZParams().aggregate(ZParams.Aggregate.MAX).weights(weights);
            String  maximum   = ind.zunion(maxParams, keys);

            String result = ind.zunion(new ZParams().weightsByDouble(2, 1, 1), base, minimum, maximum);
        }
    }
}
