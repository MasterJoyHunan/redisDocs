package redis.project;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

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
    }
}