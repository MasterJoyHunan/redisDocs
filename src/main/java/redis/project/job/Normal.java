package redis.project.job;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.ZParams;

import java.util.Set;
import java.util.UUID;

/**
 * @author joy
 * @time 2019/11/01 09:00
 */
public class Normal {

    public void indexJob(int jobId, String[] skills) {
        Transaction trans = RedisUtil.getRedis().multi();
        for (String skill : skills) {
            trans.sadd("JOB:SKILL_INDEX:" + skill, jobId + "");
        }
        trans.zadd("JOB:NEED_SKILL_LENGTH:", skills.length, jobId + "");
        trans.exec();
    }


    /**
     * 找出求职者能胜任的所有工作
     *
     * @param candidateSkills
     * @return
     */
    public Set<String> findJobs(String[] candidateSkills) {
        String[] keys = new String[candidateSkills.length];
        for (int i = 0; i < candidateSkills.length; i++) {
            keys[i] = "JOB:SKILL_INDEX:" + candidateSkills[i];
        }

        Jedis  redis     = RedisUtil.getRedis();
        String jobScores = "JOB:" + UUID.randomUUID().toString();

        // 获取所有需要该技能的招聘id的并集, 分值相加
        redis.zunionstore(jobScores, keys);

        // 获取企业需要的技能的总数，一样的就是匹配的
        redis.zinterstore(jobScores, new ZParams().weightsByDouble(-1, 1), jobScores, "JOB:NEED_SKILL_LENGTH:");
        redis.expire(jobScores, 50);

        // 技能熟练度


        // 技能使用年限

        return redis.zrangeByScore(jobScores, 0, 0);
    }


}
