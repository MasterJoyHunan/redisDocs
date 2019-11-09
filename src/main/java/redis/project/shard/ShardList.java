package redis.project.shard;

import redis.RedisUtil;
import redis.clients.jedis.Pipeline;
import redis.project.lua.LuaLoad;

import java.util.ArrayList;
import java.util.List;

/**
 * @author joy
 * @time 2019/11/08 09:24
 */
public class ShardList {

    public final static String dummy = "dfhnsgxhg#$Ttwsf1SXFGVsg";

    /**
     * 分片版 lpush
     *
     * @param key
     * @param members
     */
    public void shardLpush(String key, String[] members) {
        shardedPushHelper(key, members, "lpush");
    }

    /**
     * 分片版 rpush
     *
     * @param key
     * @param members
     */
    public void shardRpush(String key, String[] members) {
        shardedPushHelper(key, members, "rpush");
    }

    /**
     * 分片版 lpop
     *
     * @param key
     */
    public String shardLpop(String key) {
        return shardedPopHelper(key, "lpop");
    }

    /**
     * 分片版 rpop
     *
     * @param key
     */
    public String shardRpop(String key) {
        return shardedPopHelper(key, "rpop");
    }


    /**
     * 分片版 blpop
     *
     * @param key
     */
    public String shardBlpop(String key) {
        return shardBpopHelper(key, 30, "blpop");
    }

    /**
     * 分片版 brpop
     *
     * @param key
     */
    public String shardBrpop(String key) {
        return shardBpopHelper(key, 30, "brpop");
    }


    /**
     * redis 3.0 以后要的版本都不需要这样分片，因为3.0后默认使用quicklist  quicklist里包含了n个ziplist结构
     * 该分片只能在单机模式下使用，因为你不知道存入下一个分片的是哪个key
     *
     * @param key     需要操作的KEY
     * @param members 加入的member
     * @param cmd     判断是什么操作
     */
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
            // 清空所有参数
            argv.clear();

            argv.add(cmd);
            for (int i = total; i < members.length; i++) {
                // 将未加入到的元素加入参数
                argv.add(members[i]);
            }
            long joinCount = (Long) LuaLoad.scriptLoad(shardedPushLua(), keys, argv);

            // 累计已加入的数量
            total += joinCount;
        }
    }


    public String shardedPopHelper(String key, String cmd) {
        List<String> keys = new ArrayList<>();
        List<String> argv = new ArrayList<>();
        keys.add(key + ":");
        keys.add(key + ":first");
        keys.add(key + ":last");
        argv.add(cmd);
        return (String) LuaLoad.scriptLoad(shardedPopLua(), keys, argv);
    }


    /**
     * 之所以没有使用 WATCH/MULTI/EXEC 来保护相关的数据，并将 BLPOP 或者
     * BRPOP 用作执行 EXEC 之前的最后一个命令，是因为被 MULTI/EXEC 包围的 BLPOP 命令或者 BRPOP
     * 命令在遇上空列表的时候，会因为事务不允许其他客户端执行命令的原因而导致服务器一直处于被
     * 阻塞的状态。为了防止这个错误出现，客户端会把 MULTI/EXEC 包围的 BLPOP 或 BRPOP 替换成它
     * 们的非阻塞版本 LPOP 或 RPOP （除非用户给弹出操作设置了多个列表作为弹出来源）
     *
     * @param key     操作的list
     * @param timeout 阻塞时间
     * @param type    判断左右阻塞
     * @return
     */
    public String shardBpopHelper(String key, int timeout, String type) {
        Pipeline pipe = RedisUtil.getRedis().pipelined();
        timeout = Math.max(timeout, 0) <= 0 ? Math.max(timeout, 0) : 2 * 64;
        long   end          = System.currentTimeMillis() + timeout;
        String currentSharp = "blpop".equals(type) ? ":first" : ":last";
        while (System.currentTimeMillis() < end) {

            // 如果所有分片里面有数据，直接弹出就好了
            String getPop = "blpop".equals(type) ? shardLpop(key) : shardRpop(key);
            if (getPop == null || getPop.equals(dummy)) {
                return getPop;
            }

            // 所有分片里面如果没有数据，根据条件获取需要阻塞弹出的分片
            String shard = RedisUtil.getRedis().get(key + currentSharp);
            shard = shard == null ? "0" : shard;

            List<String> keys = new ArrayList<>();
            List<String> argv = new ArrayList<>();
            keys.add(key + ":");
            keys.add(key + currentSharp);

            argv.add(shard);
            argv.add("blpop".equals(type) ? "lpush" : "rpush");
            argv.add(dummy);

            // 如果程序在执行过程中，其他客户端对分片插入N条数据，导致 first/last 变化，则插入假数据
            pipe.eval(sharded_bpop_lua(), keys, argv);
            if ("blpop".equals(type)) {
                pipe.blpop(1, key + ":" + shard);
            } else {
                pipe.brpop(1, key + ":" + shard);
            }

            List<Object> res = pipe.syncAndReturnAll();
            if (res.size() == 0) {
                return null;
            }
            String result = (String) res.get(res.size() - 1);

            // 如果弹出的是空，或者是假数据，则再次循环
            if (result != null && result.equals(dummy)) {
                return result;
            }
        }
        return null;
    }

    private String shardedPushLua() {
        return "local max = 2 \n" +
                "local skey = ARGV[1] == 'lpush' and KEYS[2] or KEYS[3]  --xx:first / xx:last \n" +
                "local shard = redis.call('get', skey) or '0' -- xx:0/1/3/ \n" +
                "while 1 do --死循环\n" +
                "local current = tonumber(redis.call('llen', KEYS[1]..shard)) -- 当前分片里面有多少个元素 \n" +
                "local topush = math.min(#ARGV - 1, max - current)  -- 可以加入多少个元素进当前分片 \n" +
                "if topush > 0 then --如果可以加入元素\n" +
                "redis.call(ARGV[1], KEYS[1] .. shard, unpack(ARGV, 2, topush + 1)) --将所有元素加入 unpack\n" +
                "return topush -- 返回加入了多个元素\n" +
                "end \n" +
                "shard = redis.call(ARGV[1] == 'lpush' and 'decr' or 'incr', skey) --如果分片满了，则创建下一个分片\n" +
                "end";
    }


    private String shardedPopLua() {
        return "local skey = ARGV[1] == \"lpop\" and KEYS[2] or KEYS[3] --需要弹出的分片 first\n" +
                "local okey = ARGV[1] ~= \"lpop\" and KEYS[2] or KEYS[3] --不需要弹出的分片 last\n" +
                "local shard = redis.call(\"get\", skey) or \"0\" -- 如果找不到需要弹出的分片，默\n" +
                "local res = redis.call(ARGV[1], KEYS[1] .. shard) -- 该分分片弹出成员 （弹出的成员有可能=0|false|null|nil ）\n" +
                "if not res or redis.call(\"llen\", KEYS[1]) == \"0\" then -- 弹出失败(所有数据都清空了) 或 (该分片的数据全部弹出)\n" +
                "    local oshard = redis.call(\"get\", okey) or \"0\" -- 判断是否整个key都空了\n" +
                "    if shard == oshard then\n" +
                "        return res\n" +
                "    end\n" +
                "    local cmd = ARGV[1] == \"lpop\" and \"incr\" or \"decr\" -- 整个key没空，则选择下一个分片\n" +
                "    shard = redis.call(cmd, skey)\n" +
                "    if not res then\n" +
                "        return redis.call(ARGV[1], KEYS[1] .. shard)\n" +
                "    end\n" +
                "end\n";
    }

    private String sharded_bpop_lua() {
        return "local shard = redis.call('get', KEYS[2]) or '0'\n" +
                "if shard ~= ARGV[1] then\n" +
                "  redis.call(ARGV[2], KEYS[1]..ARGV[1], ARGV[3])\n" +
                "end\n";
    }
}

