package redis.project;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

/**
 * @author joy
 * @time 2019/09/30 08:40
 */
public class Vote {


    /**
     * 为文章投票
     *
     * @param aid 文章ID
     * @param uid 用户ID
     */
    public void articleVote(int aid, int uid) {
        Jedis redis = RedisUtil.getRedis();
        if (redis.sadd("vote:article:" + aid, uid + "") == 1) {
            redis.zincrby("score:article", Const.VOTE_SCORE, aid + "");
            redis.hincrBy("article:" + aid, "vote", 1);
        }
    }
}
