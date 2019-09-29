package redis;

import redis.clients.jedis.Jedis;

/**
 * Hello world!
 *
 */
public class App 
{
    public static void main(String[] args) {
        Jedis jedis = new Jedis("localhost");
        jedis.hset("hash", "nage", "bobo");
    }
}
