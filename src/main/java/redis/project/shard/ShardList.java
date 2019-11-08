package redis.project.shard;

import redis.project.lua.LuaLoad;

import java.util.ArrayList;
import java.util.List;

/**
 * @author joy
 * @time 2019/11/08 09:24
 */
public class ShardList {

    public void shardLpush(String key, String[] members) {
        shardedPushHelper(key, members, "lpush");
    }


    public void shardRpush(String key, String[] members) {
        shardedPushHelper(key, members, "rpush");
    }


    public void shardedPushHelper(String key, String[] members, String cmd) {
        if (members.length == 0) {
            return;
        }
        List<String> keys = new ArrayList<>();
        List<String> argv = new ArrayList<>();
        keys.add(key + ":");
        keys.add(key + ":first");
        keys.add(key + ":last");

        int total = 0;
        while (total != members.length) {
            argv.clear();
            argv.add(cmd);
            for (int i = total; i < members.length; i++) {
                argv.add(members[i]);
            }
            long joinCount = (Long) LuaLoad.scriptLoad(shardedPushLua(), keys, argv);
            System.out.println(joinCount);
            total += joinCount;
        }
    }


    public String shardedPushLua() {
        return "local max = 2 \n" +
                "local skey = ARGV[1] == 'lpush' and KEYS[2] or KEYS[3]  --xx:first / xx:last \n" +
                "local shard = redis.call('get', skey) or '0' -- xx:0/1/3/ \n" +
                "while 1 do \n" +
                "local current = tonumber(redis.call('llen', KEYS[1]..shard)) -- 当前分片里面有多少个元素 \n" +
                "local topush = math.min(#ARGV - 1, max - current)  -- 可以加入多少个元素进当前分片 \n" +
                "if topush > 0 then \n" +
                "redis.call(ARGV[1], KEYS[1] .. shard, unpack(ARGV, 2, topush + 1)) \n" +
                "return topush \n" +
                "end \n" +
                "shard = redis.call(ARGV[1] == 'lpush' and 'decr' or 'incr', skey) \n" +
                "end";
    }
}

