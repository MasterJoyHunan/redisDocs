package redis.project.pub_sub;

import redis.clients.jedis.JedisPubSub;

/**
 * @author joy
 * @time 2019/10/08 08:35
 */
public class Sub extends JedisPubSub {
    @Override
    public void onMessage(String channel, String message) {
        System.out.println(channel + " : " + message);
    }
}
