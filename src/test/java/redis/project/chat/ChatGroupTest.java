package redis.project.chat;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;
import redis.clients.jedis.Tuple;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatGroupTest {

    @Test
    public void createGroup() {
        Set<String> recipients = new HashSet<>();
        recipients.add("张三");
        recipients.add("李四");
        recipients.add("王五");
        recipients.add("赵六");
        new ChatGroup().createGroup("牛三", recipients, "牛三创建群聊,欢迎大家加入");
    }

    @Test
    public void sendMessage() {
        new ChatGroup().sendMessage(1 + "", "牛三", "boom sakalaka");
    }


    @Test
    public void getGroupMessage() {
        new ChatGroup().getGroupMessage("牛三");
    }

    @Test
    public void getGroupMessageWithLua() {
        new ChatGroup().getGroupMessageWithLua("牛三");
    }

    @Test
    public void joinChat() {
        new ChatGroup().joinChat(2, "BOBO");
    }

    @Test
    public void leaveChat() {

    }

}