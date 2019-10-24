package redis.project.search;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author joy
 * @time 2019/10/24 08:08
 */
public class InvertedIndexes {

    public static final Set<String> STOP_WORD = new HashSet<>();

    static {
        for (String word : ("able about across after all almost also am among " +
                "an and any are as at be because been but by can " +
                "cannot could dear did do does either else ever " +
                "every for from get got had has have he her hers " +
                "him his how however if in into is it its just " +
                "least let like likely may me might most must my " +
                "neither no nor not of off often on only or other " +
                "our own rather said say says she should since so " +
                "some than that the their them then there these " +
                "they this tis to too twas us wants was we were " +
                "what when where which while who whom why will " +
                "with would yet you your").split(" ")) {
            STOP_WORD.add(word);
        }
    }


    /**
     * 将标记化的文档加入redis
     *
     * @return
     */
    public int indexDocument(String id, String content) {
        Jedis       redis = RedisUtil.getRedis();
        Transaction trans = redis.multi();
        Set<String> token = tokenize(content);
        for (String word : token) {
            trans.sadd("WORD_INDEX:" + word, id);
        }
        List<Object> res = trans.exec();
        return res.size();
    }

    public String setCommon(String method, String... items) {
        String      id    = UUID.randomUUID().toString();
        Jedis       redis = RedisUtil.getRedis();
        Transaction trans = redis.multi();
        String[]    keys  = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            keys[i] = "WORD_INDEX:" + items[i];
        }
        try {
            trans.getClass()
                    .getDeclaredMethod(method, String.class, String[].class)
                    .invoke(trans, "WORD_INDEX:" + id, keys);
        } catch (Exception e) {
            throw new RuntimeException("redis method " + method + " not existent");
        }
        trans.expire("WORD_INDEX:" + id, 30);
        trans.exec();
        return id;
    }


    /**
     * 标记化去重，移除非用词
     *
     * @param content 内容
     * @return
     */
    public Set<String> tokenize(String content) {
        Set<String> token = new HashSet<>();
        for (String word : content.split(" ")) {
            if (STOP_WORD.contains(word)) {
                continue;
            }
            token.add(word);
        }
        return token;
    }

    /**
     * 求交集 a & b
     *
     * @param item
     * @return
     */
    public String intersect(String... item) {
        return setCommon("sinterstore", item);
    }

    /**
     * 求并集 a | b
     *
     * @param item
     * @return
     */
    public String union(String... item) {
        return setCommon("sunionstore", item);
    }

    /**
     * 求差集 a ^ b
     *
     * @param item
     * @return
     */
    public String difference(String... item) {
        return setCommon("sdiffstore", item);
    }
}
