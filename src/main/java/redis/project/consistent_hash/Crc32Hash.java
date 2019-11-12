package redis.project.consistent_hash;

import java.util.zip.CRC32;

/**
 * 哈希算法实现
 */
public class Crc32Hash implements HashAlgorithm {

    @Override
    public long hash(String k) {
        CRC32 crc = new CRC32();
        crc.update(k.getBytes());
        return crc.getValue();
    }
}
