package redis.project.chat;


import com.google.gson.Gson;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;
import redis.project.lock.DistributedLockV3;

import java.util.*;

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
        Jedis  redis   = RedisUtil.getRedis();
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
        Jedis  redis = RedisUtil.getRedis();
        String group = "CHAT:GROUP:" + groupId;

        // 消除竞争条件,需要加锁 -- 一般来说,当程序使用一个来自redis的值构建一个将要被添加到redis里面的值时,
        String unionId = DistributedLockV3.acquireLock(group);
        if (unionId == null) {
            return false;
        }

        try {
            // 发送消息
            long messageId   = redis.incr("CHAT:MESSAGE_INC_ID:" + groupId);
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
            DistributedLockV3.releaseLock(group, unionId);
        }
        return true;
    }


    /**
     * 获取群组未阅读的消息
     * 遍历所有参与的所有群组，去除每个群组未读消息，清理已经被所有人看过的消息
     *
     * @param recipient 登录人
     */
    public void getGroupMessage(String recipient) {
        String      seen        = "SEEN:" + recipient;
        Jedis       redis       = RedisUtil.getRedis();
        Set<Tuple>  joinedGroup = redis.zrangeWithScores(seen, 0, -1);
        Transaction trans       = redis.multi();
        for (Tuple tuple :
                joinedGroup) {
            String groupId   = tuple.getElement();
            double messageId = tuple.getScore();
            // 获取群组未读消息
            trans.zrangeByScore("CHAT:GROUP_MESSAGE:" + groupId, (messageId + 1) + "", "inf");
        }
        List<Object> messages            = trans.exec();
        Iterator     joinedGroupIterator = joinedGroup.iterator();
        Iterator     messagesIterator    = messages.iterator();

        trans = redis.multi();
        Gson parseJson = new Gson();
        while (joinedGroupIterator.hasNext()) {
            Tuple       thisGroup   = (Tuple) joinedGroupIterator.next();
            double      groupId     = thisGroup.getScore();
            Set<String> currentMsg  = (Set<String>) messagesIterator.next();
            int         maxMsgScore = 0;
            if (currentMsg.size() == 0) {
                continue;
            }

            // 更新已阅读记录
            for (String message : currentMsg) {
                String[] messageArr = parseJson.fromJson(message, String[].class);
                maxMsgScore = Math.max(maxMsgScore, Integer.parseInt(messageArr[0]));
            }
            trans.zadd("SEEN:" + recipient, maxMsgScore, groupId + "");
            trans.zadd("CHAT:GROUP:" + groupId, maxMsgScore, recipient);
        }
        trans.exec();


        // 删除所有人已看的消息
//        Tuple currentGroup = redis.zrangeWithScores("CHAT:GROUP:" + groupId, 0, 0).iterator().next();
//        trans.zremrangeByScore("CHAT:GROUP_MESSAGE:" + groupId, 0, currentGroup.getScore());
        // 使用lua最佳
    }


    /**
     * 获取群组未阅读的消息
     * 遍历所有参与的所有群组，去除每个群组未读消息，清理已经被所有人看过的消息
     *
     * @param recipient 登录人
     */
    public void getGroupMessageWithLua(String recipient) {
        Jedis redis = RedisUtil.getRedis();
        String script = "local myGroup = redis.call(\"zrange\", \"SEEN:\" .. KEYS[1], 0, -1, \"withscores\") -- 查所有我加入的群组\n" +
                "local msg = {}\n" +
                "local groupId = 0\n" +
                "for index, v in ipairs(myGroup) do --循环我加入的群组\n" +
                "    if index % 2 == 0 then\n" +
                "        -- groupId => 群组ID |  v => 已阅读的message\n" +
                "        -- 1. 返回所有未读消息\n" +
                "        local message = redis.call(\"zrangebyscore\", \"CHAT:GROUP_MESSAGE:\" .. groupId, v + 1, \"inf\", \"withscores\")\n" +
                "        -- 2. 更新 SEEN:N 和 CHAT:GROUP:N 的阅读\n" +
                "        if not (next(message) == nil) then\n" +
                "            local max_read = 0\n" +
                "            local arr_index = 1\n" +
                "            msg[groupId] = {}\n" +
                "            for index2, vv in ipairs(message) do\n" +
                "                if index2 % 2 == 0 and tonumber(vv) > max_read then\n" +
                "                    max_read = tonumber(vv)\n" +
                "                else\n" +
                "                    msg[groupId][arr_index] = vv\n" +
                "                    arr_index = arr_index + 1\n" +
                "                end\n" +
                "            end\n" +
                "            redis.call(\"zadd\", \"SEEN:\" .. KEYS[1], max_read, groupId)\n" +
                "            redis.call(\"zadd\", \"CHAT:GROUP:\" .. groupId, max_read, KEYS[1])\n" +
                "            -- 3. 删除所有人已读的message\n" +
                "            local min_not_read = redis.call(\"zrange\", \"CHAT:GROUP:\" .. groupId, 0, 0, \"withscores\")\n" +
                "            if not (next(min_not_read) == nil) then\n" +
                "                local max_score = tonumber(min_not_read[2])\n" +
                "                redis.call(\"zremrangebyscore\", \"CHAT:GROUP_MESSAGE:\" .. groupId, 0, max_score)\n" +
                "            end\n" +
                "        end\n" +
                "    else\n" +
                "        groupId = v\n" +
                "    end\n" +
                "end\n" +
                "if next(msg) == nil then\n" +
                "    return nil\n" +
                "else\n" +
                "    return cjson.encode(msg)\n" +
                "end\n";
        String res = (String)redis.eval(script, 1, recipient);
        if (res == null) {
            return;
        }
        Gson json = new Gson();
        Map<String, String> message = json.fromJson(res, Map.class);

        System.out.println(message);
    }


    /**
     * 加入群组
     *
     * @param groupId
     * @param user
     */
    public void joinChat(int groupId, String user) {
        Jedis       redis     = RedisUtil.getRedis();
        int         messageId = Integer.parseInt(redis.get("CHAT:MESSAGE_INC_ID:" + groupId));
        Transaction trans     = redis.multi();
        trans.zadd("CHAT:GROUP:" + groupId, messageId, user);
        trans.zadd("SEEN:" + user, messageId, groupId + "");
        trans.exec();
    }


    /**
     * 离开群组
     *
     * @param groupId
     * @param user
     */
    public void leaveChat(int groupId, String user) {
        Jedis       redis = RedisUtil.getRedis();
        Transaction trans = redis.multi();
        // 1.删除在某个群组的用户的数据
        // 2.删除我加入的群组记录
        // 3.如果是最后一个退群的，删除消息和消息自增ID
        // 4.删除被所有人阅读过的消息
        trans.zrem("CHAT:GROUP:" + groupId, user);
        trans.zrem("SEEN:" + user, groupId + "");
        trans.zcard("CHAT:GROUP:" + groupId);
        List<Object> res = trans.exec();
        if (res.size() == 0) {
            return;
        }
        Integer leftCount = (Integer) res.get(res.size() - 1);
        if (leftCount == 0) {
            redis.del("CHAT:GROUP_MESSAGE:" + groupId, "CHAT:MESSAGE_INC_ID:" + groupId);
        } else {
            Set<Tuple> notRead = redis.zrangeWithScores("CHAT:GROUP:" + groupId, 0, 0);
            if (notRead.size() == 0) {
                return;
            }
            Tuple notReadIndex = notRead.iterator().next();
            redis.zremrangeByScore("CHAT:GROUP_MESSAGE:", 0, notReadIndex.getScore());
        }
    }


}
