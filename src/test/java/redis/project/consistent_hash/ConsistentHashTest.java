package redis.project.consistent_hash;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/11/12 11:20
 */
public class ConsistentHashTest {

    @Test
    public void getNode() {
        List<String> nodes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            nodes.add("127.0.0.1 "+ UUID.randomUUID().toString());
        }
        ConsistentHash a = new ConsistentHash(nodes, new Crc32Hash());
        System.out.println(a.getLoop());
    }
}