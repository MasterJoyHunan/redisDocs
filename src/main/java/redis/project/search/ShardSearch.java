package redis.project.search;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.project.shard.Conn;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 分片搜索
 *
 * @author joy
 * @time 2019/11/05 17:10
 */
public class ShardSearch {


    public void getShardResults(String component, int shards, String query, String sort, Map<Integer, Map<String, String>> result) {
        Map<String, String> data;
        for (int i = 0; i < shards; i++) {
            Jedis redis = Conn.getRedisConn(String.valueOf(i));
            data = searchGetValues(redis, query, sort);
            if (data.size() == 0) {
                continue;
            }
            result.put(i, data);
        }
    }


    public Map<String, String> searchGetValues(Jedis conn, String queryString, String sort) {
        InvertedIndexes     indexes   = new InvertedIndexes();
        Map<String, Object> searchRes = indexes.searchAndSort(queryString, sort);
        List<String>        list      = (List<String>) searchRes.get("res");
        if (list.size() == 0) {
            return Collections.emptyMap();
        }
        Transaction trans = conn.multi();
        sort = sort.replace("-", "");
        Map<String, String> queryRes = new HashMap<>();
        for (String item : list) {
            // 这里其实用 get 也可以
            trans.hget("user:" + item, sort);
        }
        List<Object> exec = trans.exec();
        if (exec.size() == 0) {
            return Collections.emptyMap();
        }

        int i = 0;
        for (String item : list) {
            queryRes.put(item, (String) exec.get(i));
            i++;
        }
        return queryRes;
    }

}
