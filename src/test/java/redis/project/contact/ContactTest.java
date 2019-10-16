package redis.project.contact;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Tuple;
import redis.project.Const;

import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/16 07:56
 */
public class ContactTest {

    @Test
    public void updateContact() {
        for (int i = 0; i < 200; i++) {
            String contact = "contact_" + new Random().nextInt(150);
            System.out.println(contact);
            new Contact().updateContact(Const.UID, contact);
        }
    }

    @Test
    public void getContact() {
        List<String> res = new Contact().getContact(Const.UID);
        for (String contact : res) {
            System.out.println(contact);
        }
    }

    @Test
    public void removeContact() {
        new Contact().removeContact(Const.UID, "contact_45");
    }


    @Test
    public void simpleAutocompleteList() {
        List<String> res = new Contact().simpleAutocompleteList(Const.UID, "48");
        System.out.println(res);
    }

    @Test
    public void removeContactt() {
        Jedis redis = RedisUtil.getRedis();
        redis.zadd("z", 0 , "灭霸");
        Set<String> res = redis.zrange("z", 0, -1);
        for (String re : res) {
            System.out.println(re);
        }
    }

    @Test
    public void redisAutocompleteList() {
        Contact contact = new Contact();
        contact.redisAutocompleteList(Const.UID, "abc");
        contact.redisAutocompleteList(Const.UID, "a");
        contact.redisAutocompleteList(Const.UID, "");
//        List<String> res = new Contact().redisAutocompleteList(Const.UID, "abc");
    }

}