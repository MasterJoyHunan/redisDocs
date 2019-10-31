package redis.project.ad;

import redis.RedisUtil;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;
import redis.project.search.InvertedIndexes;

import java.util.HashMap;
import java.util.List;
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
     * 加入广告
     * 创建广告索引
     *
     * @param id        广告ID
     * @param locations 广告地理位置
     * @param content   广告关键词
     * @param type      广告投放计费属性 cpv / cpc / cpa
     * @param cost      单次计费
     */
    public void advertisementIndex(String id, String[] locations, String content, String type, double cost) {
        Transaction     trans = RedisUtil.getRedis().multi();
        InvertedIndexes index = new InvertedIndexes();

        // 将位置信息存储
        for (String location : locations) {
            trans.sadd("AD:LOCATION:" + location, id);
        }

        // 反向索引 [关键词 => id] [id=>关键词]
        Set<String> words = index.tokenize(content);
        for (String word : words) {
            trans.sadd("AD:INDEX:" + word, id);
            trans.sadd("AD:WORD:" + id, word);
        }

        // 估算浏览千次收益
        double ecpm = toECPM(type, 1000., 1., cost);

        // 记录广告计费方式
        trans.hset("AD:TYPES:", id, type);
        // 记录估算浏览千次收益
        trans.zadd("AD:ECPM:", ecpm, id);
        // 记录单次收益
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
        // 找出符合位置要求的所有广告
        String locationMatch = matchLocation(locations);

        // 将符合位置要求的所有广告按照排序ecpm排序
        String baseEcpm = new InvertedIndexes()
                .zintersect(new ZParams().weightsByDouble(0, 1), locationMatch, "AD:ECPM:");

        // 返回关键词和平均值ID
        Map<String, Object> target = finishScoring(locationMatch, baseEcpm, content);

        Transaction trans = RedisUtil.getRedis().multi();
        trans.incr("AD:SERVED:");

        // 找出收益最高的广告
        trans.zrevrange((String) target.get("base"), 0, 0);
        List<Object> res = trans.exec();

        if (res.size() == 0) {
            return null;
        }

        List<String> list = (List<String>) res.get(1);
        String id = list.iterator().next();
        if (id != null) {

        }
        
    }


    /**
     * 估算广告在1000次展示获取的收益
     *
     * @param type  按查看次数收费、点击次数收费、购买次数收费
     * @param views 查看多少次
     * @param count 点击次数/购买次数
     * @param cost  查看次数收费价格、点击次数收费价格、购买次数收费价格
     * @return
     */
    public double toECPM(String type, double views, double count, double cost) {
        switch (type) {
            case "cpa":
            case "cpc":
                return 1000. * cost * count / views;
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
        // 并集 a | b
        String[] items = new String[locations.length];
        for (int i = 0; i < locations.length; i++) {
            items[i] = "AD:LOCATION:" + locations[i];
        }
        return new InvertedIndexes().union(items);
    }


    /**
     * 计算包含了内容匹配的附加值广告
     *
     * @param matchedAds 根据地理位置匹配成功的广告
     * @param baseEcpm   范围内基本ecpm排序的广告
     * @param content    附加参数（关键词）
     */
    public Map<String, Object> finishScoring(String matchedAds, String baseEcpm, String content) {
        Map<String, Integer> bonusEcpm = new HashMap<>();
        Map<String, Object>  result    = new HashMap<>();
        InvertedIndexes      ind       = new InvertedIndexes();
        Set<String>          words     = ind.tokenize(content);
        result.put("words", words);
        for (String word : words) {
            // 匹配地理位置和关键词的交集 matchedAds & word
            String wordBonus = ind.zintersect(new ZParams().weightsByDouble(0, 1), matchedAds, "AD:WORD:" + word);
            bonusEcpm.put(wordBonus, 1);
        }

        if (bonusEcpm.size() == 0) {
            result.put("base", baseEcpm);
            return result;
        }

        String[] keys    = new String[bonusEcpm.size()];
        double[] weights = new double[bonusEcpm.size()];
        int      index   = 0;
        for (Map.Entry<String, Integer> bonus : bonusEcpm.entrySet()) {
            keys[index] = bonus.getKey();
            weights[index] = bonus.getValue();
            index++;
        }

        // 聚合交集中最小值
        ZParams minParams = new ZParams().aggregate(ZParams.Aggregate.MIN).weightsByDouble(weights);
        String  minimum   = ind.zunion(minParams, keys);

        // 聚合交集中最大值
        ZParams maxParams = new ZParams().aggregate(ZParams.Aggregate.MAX).weightsByDouble(weights);
        String  maximum   = ind.zunion(maxParams, keys);

        // 聚合交集中平均值
        ind.zunion(new ZParams().weightsByDouble(2, 1, 1), baseEcpm, minimum, maximum);
        result.put("base", baseEcpm);
        return result;
    }
}
