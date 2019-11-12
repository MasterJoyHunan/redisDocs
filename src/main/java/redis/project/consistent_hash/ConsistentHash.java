package redis.project.consistent_hash;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 哈希一致性算法
 *
 * @author joy
 * @time 2019/11/12 10:44
 */
@Getter
public class ConsistentHash {

    private final static int                   VR_NODE        = 12;
    private final static String                VR_NODE_SUFFIX = "_";
    private volatile     TreeMap<Long, String> loop           = new TreeMap<>();
    private final        HashAlgorithm         hashAlg;

    /**
     * 每个节点都分配12个节点出来
     *
     * @param nodes   所有节点
     * @param hashAlg 哈希算法
     */
    public ConsistentHash(List<String> nodes, HashAlgorithm hashAlg) {
        this.hashAlg = hashAlg;
        for (String config : nodes) {
            for (int i = 0; i < VR_NODE; i++) {
                loop.put(hashAlg.hash(config + VR_NODE_SUFFIX + i), config);
            }
        }
    }


    /**
     * 找到与该键至少大于或等于给定键，如果不存在这样的键的键说明是到底了，找第一个
     * @param key
     * @return
     */
    public String getNode(String key) {
        long hash = hashAlg.hash(key);
        Map.Entry<Long, String> locatedNode = loop.ceilingEntry(hash);
        if (locatedNode == null) {
            locatedNode = loop.firstEntry();
        }
        return locatedNode.getValue();
    }
}
