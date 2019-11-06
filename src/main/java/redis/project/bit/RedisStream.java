package redis.project.bit;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import java.io.InputStream;


/**
 * @author joy
 * @time 2019/11/04 19:21
 */
public class RedisStream extends InputStream {
    private Jedis  redis;
    private String key;
    private int    pos;

    public RedisStream(String key) {
        redis = RedisUtil.getRedis();
        this.key = key;
    }


    @Override
    public int available() {
        long len = redis.strlen(key);
        return (int) (len - pos);
    }


    @Override
    public int read() {
        byte[] block = redis.substr(key.getBytes(), pos, pos);
        if (block == null || block.length == 0) {
            return -1;
        }
        pos++;
        return block[0] & 0xff;
    }


    @Override
    public int read(byte[] buf, int off, int len) {
        byte[] block = redis.substr(key.getBytes(), pos, pos + (len - off - 1));
        if (block == null || block.length == 0) {
            return -1;
        }
        System.arraycopy(block, 0, buf, off, block.length);
        pos += block.length;
        return block.length;
    }
}
