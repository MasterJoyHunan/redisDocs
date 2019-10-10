package redis.project.replication;

import redis.clients.jedis.Jedis;

import java.util.HashMap;
import java.util.Map;

/**
 * @author joy
 * @time 2019/10/10 08:20
 */
public class WaitForSync {


    /**
     * 判断主从是否同步
     *
     * @param master 主redis
     * @param slave  从redis
     * @return boolean 是否已经同步
     */
    public static boolean isSync(Jedis master, Jedis slave) {
        int unionId = (int) (System.currentTimeMillis() / 1000);
        master.zadd("sync:wait", unionId, unionId + "");

        Map<String, String> replication = parseInfo(slave.info("replication"));
        while (!"up".equals(replication.get("master_link_status"))) {
            System.out.println(!"up".equals(replication.get("master_link_status")));
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        while (slave.zscore("sync:wait", unionId + "") == null) {
            System.out.println(slave.zscore("sync:wait", unionId + ""));
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        Long deadLine = System.currentTimeMillis() + 1000;
        while (System.currentTimeMillis() < deadLine) {
            System.out.println(3);
            Map<String, String> persistence = parseInfo(slave.info("persistence"));
            if ("0".equals(persistence.get("aof_pending_bio_fsync"))) {
                break;
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        master.zrem("sync:wait", unionId + "");
        int end = (int) (System.currentTimeMillis() / 1000 - 60);
        master.zremrangeByScore("sync:wait", 0, end);
        return true;
    }


    /**
     * 解析 redis info 返回 map
     *
     * @param info string
     * @return map
     */
    private static Map<String, String> parseInfo(String info) {
        Map<String, String> map     = new HashMap<>();
        String[]            infoArr = info.split("\n");
        for (String s1 : infoArr) {
            if (!s1.contains(":")) {
                continue;
            }
            String[] kvArr = s1.split(":");
            map.put(kvArr[0].trim(), kvArr[1].trim());
        }
        return map;
    }
}
