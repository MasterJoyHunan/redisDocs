package redis.project;


import com.google.gson.Gson;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;
import redis.project.lock.DistributedLock;
import redis.project.lock.DistributedLockV2;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 群组聊天
 */
public class ChatGroup {


    /**
     * 创建聊天群
     *
     * @param sender     发送人
     * @param recipients 接收人
     * @param message    消息
     * @return
     */
    public boolean createGroup(String sender, Set<String> recipients, String message) {
        // 创建群组ID
        Jedis redis = RedisUtil.getRedis();
        String groupId = redis.incr("CHAT:GROUP_INC_ID:") + "";
        recipients.add(sender);
        Transaction trans = redis.multi();
        for (String recipient :
                recipients) {
            // 群组加了那些人
            trans.zadd("CHAT:GROUP:" + groupId, 0, recipient);
            // 这些人加了那些群组
            trans.zadd("SEEN:" + recipient, 0, groupId);
        }
        trans.exec();
        return sendMessage(groupId, sender, message);
    }


    /**
     * 发送消息
     *
     * @param groupId 群ID
     * @param sender  发送人
     * @param message 消息
     * @return
     */
    public boolean sendMessage(String groupId, String sender, String message) {
        Jedis redis = RedisUtil.getRedis();
        String group = "CHAT:GROUP:" + groupId;

        // 消除竞争条件,需要加锁 -- 一般来说,当程序使用一个来自redis的值构建一个将要被添加到redis里面的值时,
        String unionId = DistributedLockV2.acquireLock(group);
        if (unionId == null) {
            return false;
        }

        try {
            // 发送消息
            long messageId = redis.incr("CHAT:MESSAGE_INC_ID:" + groupId);
            long currentTime = System.currentTimeMillis();
            String sendMessage = new Gson().toJson(new String[]{
                    messageId + "",
                    currentTime + "",
                    sender,
                    message
            });
            redis.zadd("CHAT:GROUP_MESSAGE:" + groupId, messageId, sendMessage);
        } finally {
            //解锁
            DistributedLock.releaseLock(group, unionId);
        }

        return true;
    }


    /**
     * 获取群组未阅读的消息
     * @param recipient 登录人
     */
    public void getGroupMessage(String recipient) {
        String seen = "seen:" + recipient;
        Jedis redis = RedisUtil.getRedis();
        Set<Tuple> notRead = redis.zrangeWithScores(seen, 0, -1);
        Transaction trans = redis.multi();
        for (Tuple tuple :
                notRead) {
            String groupId = tuple.getElement();
            double messageId = tuple.getScore();
            // 获取群组未读消息
            trans.zrangeByScore("CHAT:GROUP_MESSAGE:" + groupId, (messageId - 1) + "", "inf");
        }
        List<Object> messages = trans.exec();
        for (Object message :
                messages) {
            
        }

    }
}
