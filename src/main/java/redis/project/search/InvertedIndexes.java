package redis.project.search;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.SortingParams;
import redis.clients.jedis.Transaction;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author joy
 * @time 2019/10/24 08:08
 */
public class InvertedIndexes {

    public static final Set<String> STOP_WORD     = new HashSet<>();
    public static final Pattern     INDEX_PATTERN = Pattern.compile("[A-Za-z']{2,}");
    public static final Pattern     QUREY_PATTERN = Pattern.compile("[+-]?[a-zA-Z']{2,}");


    static {
        // 非用词
        String stopWord = "able about across after all almost also am among " +
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
                "with would yet you your";
        for (String word : (stopWord).split(" ")) {
            STOP_WORD.add(word);
        }
    }


    /**
     * 将标记化的文档加入redis
     *
     * @param id      文章ID
     * @param content 文章内容
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


    /**
     * 搜索
     *
     * @param query 字符串
     * @return
     */
    public String parseAndSearch(String query) {
        Map<String, Object> res      = parseQuery(query);
        List<List<String>>  search   = (List<List<String>>) res.get("all");
        Set<String>         unwanted = (Set<String>) res.get("unwanted");
        if (search.isEmpty()) {
            return null;
        }
        List<String> toIntersect = new ArrayList<>();
        for (List<String> current : search) {
            if (current.size() > 1) {
                toIntersect.add(union(current.toArray(new String[current.size()])));
            } else {
                toIntersect.add(union(current.get(0)));
            }
        }
        String intersectResult = null;
        if (toIntersect.size() == 1) {
            intersectResult = toIntersect.get(0);
        } else {
            intersectResult = union(toIntersect.toArray(new String[toIntersect.size()]));
        }

        if (!unwanted.isEmpty()) {
            String[] keys = unwanted.toArray(new String[unwanted.size() + 1]);
            keys[keys.length - 1] = intersectResult;
            intersectResult = difference(keys);
        }
        return intersectResult;
    }


    /**
     * 根据搜索进行排序
     *  @param queryString 搜索字符串
     * @param sort        排序字段
     */
    public Map searchAndSort(String queryString, String sort) {
        String        id            = parseAndSearch(queryString);
        SortingParams sortingParams = new SortingParams();

        // 排序字段前面有 - 号代表倒序排列
        if (sort.startsWith("-")) {
            sortingParams.desc();
            sort = sort.substring(1);
        }

        // 如果排序选择的是字符串字段，则按字符串继续排序， 如果选择的是数字，则按数字排序
        if ("name".equals(sort)) {
            sortingParams.alpha();
        }

        // 按照hash里该字段进行排序
        String by = "user:*->" + sort;
        sortingParams.by(by);

        // 返回hash里对应的字段
        String get = "name";
        sortingParams.get(get);

        // 最多返回20条数据
        sortingParams.limit(0, 20);

        Transaction trans = RedisUtil.getRedis().multi();
        trans.scard("WORD_SEARCH:" + id);
        trans.sort("WORD_SEARCH:" + id, sortingParams);
        List<Object>        results   = trans.exec();
        Map<String, Object> returnRes = new HashMap<>();
        returnRes.put("id", id);
        returnRes.put("number", results.get(0));
        returnRes.put("res", results.get(1));
        return returnRes;
    }

    /**
     * 解析传过来的搜索词
     *
     * @param query
     * @return
     */
    public Map<String, Object> parseQuery(String query) {
        // 不包含的单词
        Set<String> unwanted = new HashSet<>();
        // 同义词
        Set<String> current = new HashSet<>();
        // 结果集
        List<List<String>> all = new ArrayList<>();

        Matcher matcher = QUREY_PATTERN.matcher(query);
        while (matcher.find()) {
            String word   = matcher.group().toLowerCase();
            char   prefix = word.charAt(0);
            if (prefix == '+' || prefix == '-') {
                word = word.substring(1);
            }
            if (word.length() <= 2 || STOP_WORD.contains(word)) {
                continue;
            }
            // 希望在结果集中不含有该单词
            if (prefix == '-') {
                unwanted.add(word);
                continue;
            }
            // 某个单词前面是 + 则表示该单词和该单词的前一个单词是同义词
            // 该单词的前一个单词的前面是 - 则找前前，以此类推
            if (!current.isEmpty() && prefix != '+') {
                all.add(new ArrayList<>(current));
                current.clear();
            }
            current.add(word);
        }
        if (!current.isEmpty()) {
            all.add(new ArrayList<>(current));
        }
        Map<String, Object> result = new HashMap<>();
        result.put("all", all);
        result.put("unwanted", unwanted);
        return result;
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
            Matcher matcher = INDEX_PATTERN.matcher(word);
            if (!matcher.find()) {
                continue;
            }
            word = matcher.group().toLowerCase();
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

    /**
     * 返回需查询的数据
     *
     * @param method 调用方法
     * @param items  要查询的数据
     * @return
     */
    private String setCommon(String method, String... items) {
        String      id    = UUID.randomUUID().toString();
        Transaction trans = RedisUtil.getRedis().multi();
        String[]    keys  = new String[items.length];
        for (int i = 0; i < items.length; i++) {
            keys[i] = "WORD_INDEX:" + items[i];
        }
        try {
            trans.getClass()
                    .getMethod(method, String.class, String[].class)
                    .invoke(trans, "WORD_SEARCH:" + id, keys);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        trans.expire("WORD_SEARCH:" + id, 30);
        trans.exec();
        return id;
    }

}
