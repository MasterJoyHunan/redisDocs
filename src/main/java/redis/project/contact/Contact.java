package redis.project.contact;

import lombok.NonNull;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * @author joy
 * @time 2019/10/16 07:47
 */
public class Contact {

    public static final String CHARACTERS = "`abcdefghijklmnopqrstuvwxyz{";

    /**
     * 保存最近100个联系人
     *
     * @param uid     用户ID
     * @param contact 联系人
     */
    public void updateContact(int uid, String contact) {
        Jedis redis = RedisUtil.getRedis();
        Transaction trans = redis.multi();
        String key = "CONTACT:" + uid + "_recent";
        trans.lrem(key, 0, contact);
        trans.lpush(key, contact);
        trans.ltrim(key, 0, 99);
        trans.exec();
    }

    /**
     * 获取最近联系100人
     *
     * @param uid 用户ID
     * @return List<String>
     */
    public List<String> getContact(int uid) {
        Jedis redis = RedisUtil.getRedis();
        String key = "CONTACT:" + uid + "_recent";
        return redis.lrange(key, 0, -1);
    }


    /**
     * 删除最近联系人
     *
     * @param uid     用户ID
     * @param contact 联系人
     */
    public void removeContact(int uid, String contact) {
        Jedis redis = RedisUtil.getRedis();
        String key = "CONTACT:" + uid + "_recent";
        redis.lrem(key, 0, contact);
    }


    /**
     * 简易的自动完成
     *
     * @param uid    用户ID
     * @param prefix 联系人
     * @return List<String>
     */
    public List<String> simpleAutocompleteList(int uid, String prefix) {
        List<String> res = getContact(uid);
        List<String> newRes = new ArrayList<>();
        for (String contact : res) {
            if (contact.contains(prefix)) {
                newRes.add(contact);
            }
        }
        return newRes;
    }

    /**
     * 在redis中使用zset筛选数据
     * 前提：名字不能为数字和标点符号
     * 例如：用户输入 abc 在redis的 zset 中就是查 abb{ - abc{ 之间的数据
     *
     * @param uid    用户ID
     * @param prefix 搜索数据
     * @return List<String>
     */
    public List<String> redisAutocompleteList(int uid, String guild, String prefix) {
        String[] range = findPrefixRange(prefix);
        String unique = UUID.randomUUID().toString();
        String start = range[0] + unique;
        String end = range[1] + unique;
        String guildKey = "GUILD:" + guild;
        Jedis redis = RedisUtil.getRedis();
        redis.zadd(guildKey, 0, start);
        redis.zadd(guildKey, 0, end);
        while (true) {
            redis.watch(guildKey);
            int startIndex = redis.zrank(guildKey, start).intValue();
            int endIndex = redis.zrank(guildKey, end).intValue();
            endIndex = Math.min(startIndex + 9, endIndex - 2);

            Transaction trans = redis.multi();
            trans.zrem(guildKey, start, end);
            trans.zrange(guildKey, startIndex, endIndex);
            List<Object> res = trans.exec();
            if (res.size() == 0) {
                continue;
            }
            Set<String> getRange = (Set<String>) res.get(1);
            for (String re :
                    getRange) {
                System.out.println(re);
            }
            return null;
        }
    }


    /**
     * 根据前缀获取ASCII码上下边界(ASCII码部分排序)
     * 十进制   图形
     * 96       `
     * 97       a
     * 98       b
     * 99       c
     * 100      d
     * 101      e
     * 102      f
     * 103      g
     * 104      h
     * 105      i
     * 106      j
     * 107      k
     * 108      l
     * 109      m
     * 110      n
     * 111      o
     * 112      p
     * 113      q
     * 114      r
     * 115      s
     * 116      t
     * 117      u
     * 118      v
     * 119      w
     * 120      x
     * 121      y
     * 122      z
     * 123      {
     *
     * @param prefix 前缀
     * @return String[]
     */
    private String[] findPrefixRange(String prefix) {
        // abc => [ "abb{", "abc{" ]
        int position = prefix.length() - 1;
        String pre = prefix.substring(0, position);
        String start = pre + CHARACTERS.charAt(CHARACTERS.indexOf(prefix.charAt(position)) - 1) + "{";
        String end = prefix + '{';
        return new String[]{start, end};
    }


}
