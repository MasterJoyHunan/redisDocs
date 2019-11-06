package redis.project.social;

import com.google.gson.Gson;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;
import redis.project.lock.DistributedLockV3;
import redis.project.logs.Log;
import redis.project.queque.QueueDTO;
import redis.project.queque.ZsetQueue;

import java.util.*;

/**
 * @author joy
 * @time 2019/11/01 11:50
 */
public class User {

    // 最多保留1000个数据
    public static final int READ_MAX = 1000;

    /**
     * @param login 登录账号
     * @param name  昵称
     * @return
     */
    public long createUser(String login, String name) {
        Jedis redis = RedisUtil.getRedis();
        login = login.toLowerCase();

        // 尝试加锁，同一时期是否有另外的人注册同一账号
        String lock = DistributedLockV3.acquireLock("user:" + login);
        if (lock == null) {
            return -1;
        }

        // 判断该账号是否已注册
        String hasUser = redis.hget("USERS:", login);
        if (hasUser != null) {
            DistributedLockV3.releaseLock("user:" + login, lock);
            return -1;
        }

        // 获取唯一ID
        long        id    = redis.incr("USER:INC_ID:");
        Transaction trans = redis.multi();
        trans.hset("USERS:ID", login, id + "");
        Map<String, String> userInfo = new HashMap<>();
        userInfo.put("id", id + "");
        userInfo.put("login", login);
        userInfo.put("name", name);
        // 关注我的人
        userInfo.put("followers", "0");
        // 我关注的人
        userInfo.put("following", "0");
        // 发送的消息
        userInfo.put("posts", "0");
        userInfo.put("create_time", System.currentTimeMillis() + "");
        trans.hmset("USERS:ID" + id, userInfo);
        List<Object> res = trans.exec();
        if (res.size() == 0) {
            return -1;
        }
        DistributedLockV3.releaseLock("user:" + login, lock);
        return id;
    }


    /**
     * @param uid     用户ID
     * @param message 发送的消息
     */
    public long createStatus(long uid, String message) {
        Jedis        redis = RedisUtil.getRedis();
        Transaction  trans;
        List<Object> res;
        trans = redis.multi();
        trans.incr("STATUS:INC_ID:");
        trans.hget("USERS:ID:" + uid, "login");
        res = trans.exec();
        if (res.size() == 0) {
            return -1;
        }
        long   statusId = (long) res.get(0);
        String login    = (String) res.get(1);

        // 该用户还未注册
        if (login == null) {
            return -1;
        }

        Map<String, String> data = new HashMap<>();
        data.put("id", String.valueOf(statusId));
        data.put("uid", String.valueOf(uid));
        data.put("message", message);
        data.put("create_time", String.valueOf(System.currentTimeMillis()));

        trans = redis.multi();
        trans.hmset("STATUS:ID:" + statusId, data);
        trans.hincrBy("USERS:ID:" + uid, "posts", 1);
        res = trans.exec();
        if (res.size() == 0) {
            return -1;
        }
        return statusId;
    }


    /**
     * 从时间线里面获取给定页数的最新状态消息
     *
     * @param uid 用户ID
     * @return
     */
    public List<Map<String, String>> statusMessage(long uid) {
        Jedis       redis     = RedisUtil.getRedis();
        Transaction trans     = redis.multi();
        Set<String> statusIds = redis.zrevrange("STATUS:HOME:" + uid, 0, 30);

        // 关联消息
        for (String statusId : statusIds) {
            trans.hgetAll("STATUS:ID:" + statusId);
        }
        List<Object> list = trans.exec();
        if (list.size() == 0) {
            return Collections.emptyList();
        }

        // 过滤
        List<Map<String, String>> returnResult = new ArrayList<>();
        for (Object temp : list) {
            Map<String, String> item = (Map<String, String>) temp;
            if (item.size() == 0) {
                continue;
            }
            returnResult.add(item);
        }
        return returnResult;
    }


    /**
     * 关注其他用户
     *
     * @param uid     操作人
     * @param otherId 被关注人 ID
     * @return
     */
    public boolean followUser(long uid, long otherId) {
        // 我关注的人
        String following = "USERS:FOLLOWING:" + uid;
        // 我关注的人 - 关注他的人
        String followers = "USERS:FOLLOWERS:" + otherId;

        Transaction  trans;
        List<Object> execResult;

        Jedis  redis    = RedisUtil.getRedis();
        Double isFollow = redis.zscore(following, String.valueOf(otherId));
        if (isFollow != null) {
            return false;
        }
        trans = redis.multi();
        long time = System.currentTimeMillis();
        // 加入我关注的人
        trans.zadd(following, time, String.valueOf(otherId));
        // 加入我关注的人 - 他的关注他的人
        trans.zadd(followers, time, String.valueOf(uid));
        // 我关注的人 + 1
        trans.hincrBy("USERS:ID:" + uid, "following", 1);
        // 我关注的人 - 他的关注他的人 + 1
        trans.hincrBy("USERS:ID:" + otherId, "followers", 1);
        // 获取我关注的人发表的状态 最多获取1000条
        trans.zrevrangeWithScores("STATUS:PROFILE:" + otherId, 0, READ_MAX - 1);
        execResult = trans.exec();
        if (execResult.size() == 0) {
            return false;
        }

        // 将我关注的人发表的状态加入到我的主页
        Set<Tuple> otherStatus = (Set<Tuple>) execResult.get(execResult.size() - 1);
        if (otherStatus.size() == 0) {
            return true;
        }
        trans = redis.multi();
        for (Tuple item : otherStatus) {
            trans.zadd("STATUS:HOME:" + uid, item.getScore(), item.getElement());
        }

        // 只保留1000条数据
        trans.zremrangeByRank("STATUS:HOME:" + uid, 0, -READ_MAX - 1);
        trans.exec();
        return true;
    }


    /**
     * 取消关注其他用户
     *
     * @param uid     操作人
     * @param otherId 被关注人 ID
     * @return
     */
    public boolean unfollowUser(long uid, long otherId) {
        // 我关注的人
        String following = "USERS:FOLLOWING:" + uid;
        // 我关注的人 - 关注他的人
        String followers = "USERS:FOLLOWERS:" + otherId;

        Transaction  trans;
        List<Object> execResult;

        Jedis  redis    = RedisUtil.getRedis();
        Double isFollow = redis.zscore(following, String.valueOf(otherId));
        if (isFollow == null) {
            return false;
        }
        trans = redis.multi();
        trans.zrem(following, String.valueOf(otherId));
        trans.zrem(followers, String.valueOf(uid));
        trans.hincrBy("USERS:ID:" + uid, "following", -1);
        trans.hincrBy("USERS:ID:" + otherId, "followers", -1);
        trans.zrevrangeByScore("STATUS:PROFILE:" + otherId, 0, READ_MAX - 1);
        execResult = trans.exec();
        if (execResult.size() == 0) {
            return false;
        }

        Set<Tuple> otherStatus = (Set<Tuple>) execResult.get(execResult.size() - 1);
        if (otherStatus.size() == 0) {
            return true;
        }
        trans = redis.multi();
        for (Tuple item : otherStatus) {
            trans.zrem("STATUS:PROFILE:" + uid, item.getElement());
        }

        // 用户主页的数据可能会少一部分，通过队列、延时队列尽快填补空缺
        QueueDTO data = new QueueDTO("redis.project.social.FixUserHome", "fix", uid);
        ZsetQueue.laterQueue("LEVEL_3_DELAYED", new Gson().toJson(data), 1000);

        trans.exec();
        return true;
    }


    /**
     * 发送消息
     *
     * @param uid     用户id
     * @param message 发送的消息
     * @return
     */
    public boolean postStatus(long uid, String message) {
        long statusId = createStatus(uid, message);
        if (statusId == -1) {
            return false;
        }

        // 判断是否插入
        Jedis  redis      = RedisUtil.getRedis();
        String createTime = redis.hget("STATUS:ID:" + statusId, "create_time");
        if (createTime == null) {
            return false;
        }

        // 将发的消息插入 我发布的消息
        Transaction trans = redis.multi();
        trans.zadd("STATUS:PROFILE:" + uid, Long.valueOf(createTime), String.valueOf(statusId));
        trans.zadd("STATUS:HOME:" + uid, Long.valueOf(createTime), String.valueOf(statusId));
        List<Object> execResult = trans.exec();
        if (execResult.size() == 0) {
            return false;
        }

        // 对关注我的人的时间线更新
        Set<Tuple> home = redis.zrangeByScoreWithScores("USERS:FOLLOWERS:" + uid, "0", "inf", 0, READ_MAX - 1);
        if (home.size() == 0) {
            return true;
        }
        trans = redis.multi();
        for (Tuple item : home) {
            // 更新关注我的人的主页
            trans.zadd("STATUS:HOME:" + item.getElement(), item.getScore(), String.valueOf(statusId));
            trans.zremrangeByRank("STATUS:HOME:" + item.getElement(), 0, -READ_MAX - 1);
        }
        trans.exec();
        if (home.size() >= READ_MAX) {
            // 粉丝超过1000人的会员 发送延迟队列服务
            QueueDTO data = new QueueDTO("redis.project.social.FixUserHome", "pushOther", uid);
            ZsetQueue.laterQueue("LEVEL_3_DELAYED", new Gson().toJson(data), 1000);
        }
        return true;
    }


    /**
     * 删除状态
     *
     * @param uid      用户
     * @param statusId 广告
     * @return
     */
    public boolean deleteStatus(long uid, long statusId) {
        String id    = DistributedLockV3.acquireLock("del_status" + statusId);
        Jedis  redis = RedisUtil.getRedis();
        if (id == null) {
            return false;
        }

        // 如果该数据已被删除，或该数据不是本人发的。
        String statusUid = redis.hget("STATUS:ID:" + statusId, "uid");
        if (statusUid == null || Long.valueOf(statusUid) != uid) {
            DistributedLockV3.releaseLock("del_status" + statusId, id);
            return false;
        }

        Transaction trans = redis.multi();
        trans.zrem("STATUS:PROFILE:" + uid, String.valueOf(statusId));
        trans.zrem("STATUS:HOME:" + uid, String.valueOf(statusId));
        trans.del("STATUS:ID:" + statusId);
        trans.hincrBy("USERS:ID" + uid, "posts", -1);
        List<Object> res = trans.exec();

        // 释放锁
        DistributedLockV3.releaseLock("del_status" + statusId, id);
        // 延迟队列 对关注过我的人对home删减
        QueueDTO data = new QueueDTO("redis.project.social.FixUserHome", "deleteStatus", uid);
        ZsetQueue.laterQueue("LEVEL_3_DELAYED", new Gson().toJson(data), 1000);

        return res.size() != 0;
    }
}
