package redis.project.ad;

import redis.RedisUtil;
import redis.clients.jedis.Transaction;
import redis.project.search.InvertedIndexes;

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
}
