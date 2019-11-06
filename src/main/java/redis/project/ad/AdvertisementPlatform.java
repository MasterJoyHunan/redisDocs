package redis.project.ad;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;
import redis.project.search.InvertedIndexes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 广告定向平台
 * -----云里雾里 TODO 无法使用
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
        trans.zadd("AD:BASE_COST:", cost, id);
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
        String       id   = list.iterator().next();
        if (id == null) {
            return null;
        }
        recordTargetingResult((Long) res.get(0), id, (Set<String>) target.get("word"));
        return id;
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
            // word => 假设我们为关键词做了附加值操作
            String wordBonus = ind.zintersect(new ZParams().weightsByDouble(0, 1), matchedAds, "AD:WORD_EXT:" + word);
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

        // 聚合并集中最小值 a | b
        ZParams minParams = new ZParams().aggregate(ZParams.Aggregate.MIN).weightsByDouble(weights);
        String  minimum   = ind.zunion(minParams, keys);

        // 聚合并集中最大值 a | b
        ZParams maxParams = new ZParams().aggregate(ZParams.Aggregate.MAX).weightsByDouble(weights);
        String  maximum   = ind.zunion(maxParams, keys);

        // 聚合并集中平均值 a | b
        ind.zunion(new ZParams().weightsByDouble(1, .5, .5), baseEcpm, minimum, maximum);
        result.put("base", baseEcpm);
        return result;
    }


    /**
     * @param targetId 上下文ID
     * @param adId     被推荐的广告ID
     * @param words    关键词
     */
    public void recordTargetingResult(long targetId, String adId, Set<String> words) {
        Jedis redis = RedisUtil.getRedis();

        // 该广告的关键词
        Set<String> terms = redis.smembers("AD:WORD:" + adId);

        // 获取该广告的计费方式
        String type = redis.hget("AD:TYPES:", adId);

        Transaction trans = redis.multi();

        // 广告的关键词 + 当前搜索的关键词
        terms.addAll(words);
        if (terms.size() > 0) {
            // 不知道这是干嘛的
            String matchedKey = "AD:MATCHED:" + targetId;
            for (String term : terms) {
                trans.sadd(matchedKey, term);
                trans.zincrby("AD:VIEWS:" + adId, 1, term);
            }
            trans.expire(matchedKey, 900);
        }

        // 该广告类型的广告查看的次数
        trans.incr("AD:TYPE_VIEWS:" + type);

        // 将该广告匹配的关键词次数 + 1 自己ID的次数 + 1
        trans.zincrby("AD:VIEWS:" + adId, 1, "");

        List<Object> response = trans.exec();
        double       views    = (Double) response.get(response.size() - 1);

        // 100 为匹配单次计算附加值
        if ((views % 100) == 0) {
//            updateCpms(conn, adId);
        }
    }


    /**
     * 广告被点击执行
     *
     * @param targetId 上下文ID
     * @param adId     广告ID
     * @param action   是否执行了操作(有时候，点击了广告并没有在里面进行操作)
     */
    public void recordClick(long targetId, String adId, boolean action) {
        Jedis       redis    = RedisUtil.getRedis();
        String      clickKey = "AD:CLICKS:" + adId;
        String      matchKey = "AD:MATCHED:" + targetId;
        String      adType   = redis.hget("AD:TYPES:", adId);
        Transaction trans    = redis.multi();
        Set<String> matched  = redis.smembers(matchKey);
        matched.add("");
        if ("cpa".equals(adType)) {
            trans.expire(matchKey, 900);
            if (action) {
                clickKey = "AD:ACTIONS:" + adId;
            }
        }

        // 如果点击广告进去，并且进行了操作
        if (action && "cpa".equals(adType)) {
            trans.incr("AD:" + adType + ":ACTION");
        } else {
            trans.incr("AD:" + adType + ":CLICKS");
        }

        for (String word : matched) {
            trans.zincrby(clickKey, 1, word);
        }
        trans.exec();
    }


    /**
     * 更新单次的附加值
     *
     * @param adId
     */
    public void updateCpms(String adId) {
        Jedis        redis = RedisUtil.getRedis();
        Transaction  trans;
        List<Object> res;
        // 查看广告类型
        // 查看广告单价
        // 查广告所有关键词
        trans = redis.multi();
        trans.hget("AD:TYPES:", adId);
        trans.zscore("AD:BASE_COST:", adId);
        trans.smembers("AD:WORD:" + adId);
        res = trans.exec();
        if (res.size() == 0) {
            return;
        }
        String   type      = (String) res.get(0);
        double   base_cost = (double) res.get(1);
        String[] words     = (String[]) res.get(2);

        // 获取广告类型被匹配多少次
        // 获取广告类型（操作量）次数
        trans = redis.multi();
        String which = "cpa".equals(type) ? "ACTION" : "CLICKS";
        trans.get("AD:TYPE_VIEWS:" + type);
        trans.get("AD:" + type + ":" + which);
        trans.exec();
        res = trans.exec();
        if (res.size() == 0) {
            return;
        }

        int ecpm = res.get(0) == null ? (int) res.get(0) : 1;
        int avg  = res.get(1) == null ? (int) res.get(1) : 1;

        Map<String, Double> AVG_1K = new HashMap<>();
        AVG_1K.put(type, 1000.0 * avg / ecpm);
        if ("cpm".equals(type)) {
            return;
        }

        // 广告被查看的次数
        // 广告被操作的次数
        String viewKey  = "AD:VIEWS:" + adId;
        String clickKey = "AD:" + which + ":" + adId;
        trans = redis.multi();
        trans.zscore(viewKey, "");
        trans.zscore(clickKey, "");
        res = trans.exec();
        if (res.size() == 0) {
            return;
        }

        int idViews = res.get(0) == null ? 0 : (int) res.get(0);
        int idClick = res.get(1) == null ? 0 : (int) res.get(1);

        // 没人点击
        double ad_ecpm = 0;
        trans = redis.multi();
        if (idClick < 1) {
            ad_ecpm = redis.zscore("AD:ECPM:", adId);
        } else {
            ad_ecpm = toECPM(type, idViews, idClick, base_cost);
            trans.zadd("AD:ECPM:", ad_ecpm, adId);
        }
        trans.exec();

        for (String word : words) {
            trans.zscore(viewKey, word);
            trans.zscore(clickKey, word);
            res = trans.exec();
            if (res.size() == 0) {
                continue;
            }

            double views    = res.get(0) == null ? (double) res.get(0) : 1;
            double click    = res.get(1) == null ? (double) res.get(1) : 0;
            double wordEcpm = toECPM(type, views, click, ecpm);
            double bonus = wordEcpm - ecpm;
            // 为广告关键词添加附加值
            redis.zadd("AD:WORD_EXT:"+word, bonus, adId);
        }

    }
}
