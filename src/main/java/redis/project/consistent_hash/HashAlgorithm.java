package redis.project.consistent_hash;

/**
 * 哈希算法接口
 */
public interface HashAlgorithm {
    long hash(final String k);
}
