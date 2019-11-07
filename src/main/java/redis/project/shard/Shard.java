package redis.project.shard;

import java.util.zip.CRC32;

/**
 * redis 分片
 *
 * @author joy
 * @time 2019/11/04 10:04
 */
public class Shard {

    /**
     * @param key          基础名字 (hash set zset list) 不包含string string有单独分片机制
     * @param member       将要被储存到分片里面的键 (hash=>member, set=>member, zset=>member, list=>item)
     * @param totalElement 预计元素总数量
     * @param shardSize    每个分片里包含的数量
     * @return
     */
    public static String shardKey(String key, String member, int totalElement, int shardSize) {
        boolean isNumber = true;
        int     shardId;
        for (char c : member.toCharArray()) {
            if (!Character.isDigit(c)) {
                isNumber = false;
                break;
            }
        }
        if (isNumber) {
            shardId = Integer.parseInt(member, 10) / shardSize;
        } else {
            int   shards = 2 * totalElement / shardSize;
            CRC32 crc    = new CRC32();
            crc.update(member.getBytes());
            shardId = Math.abs(((int) crc.getValue()) % shards);
        }
        return key + ":" + shardId;
    }

}
