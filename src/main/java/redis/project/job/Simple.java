package redis.project.job;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author joy
 * @time 2019/11/01 08:36
 */
public class Simple {

    /**
     * 添加招聘需要
     *
     * @param jobId
     * @param needSkills
     */
    public void addJob(int jobId, String[] needSkills) {
        RedisUtil.getRedis().sadd("JOB:" + jobId, needSkills);
    }


    public boolean findJob(int jobId, String[] candidateSkills) {
        Jedis       redis = RedisUtil.getRedis();
        Transaction trans = redis.multi();
        String      temp  = UUID.randomUUID().toString();
        trans.sadd(temp, candidateSkills);
        trans.expire(temp, 5);
        trans.sdiff( "JOB:" + jobId, temp);
        List<Object> res = trans.exec();
        if (res.size() == 0) {
            return false;
        }
        Set<String> diff = (Set<String>)res.get(2);
        if (diff.size() == 0) {
            return true;
        }
        return false;
    }
}
