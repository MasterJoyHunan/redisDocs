package redis.project.queque;

import com.google.gson.Gson;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.project.logs.Log;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * 队列服务
 *
 * @author joy
 * @time 2019/10/18 09:49
 */
public class Queue extends Thread {


    /**
     * 发送邮件 人队
     *
     * @param fromId 发送人
     * @param toId   接受人
     * @param email  邮件内容
     */
    public void sendEmailViaQueue(int fromId, int toId, String email) {
        Jedis redis = RedisUtil.getRedis();
        List<Object> args = new ArrayList<>();
        args.add(fromId);
        args.add(toId);
        args.add(email);
        QueueDTO queueDTO = new QueueDTO("redis.project.queque.TestCallBack", "testMethod", args);
        redis.rpush("QUEUE:LEVEL_1", new Gson().toJson(queueDTO));
    }

    public volatile boolean quit = false;

    /**
     * 将邮件队列中数据的弹出
     */
    @Override
    public void run() {
        Jedis redis = RedisUtil.getRedis();
        while (!quit) {
            // 按优先级弹出消息队列
            List<String> emailData = redis.blpop(30,
                    "QUEUE:LEVEL_1_DELAYED",
                    "QUEUE:LEVEL_1",
                    "QUEUE:LEVEL_2_DELAYED",
                    "QUEUE:LEVEL_2",
                    "QUEUE:LEVEL_3_DELAYED",
                    "QUEUE:LEVEL_3");
            if (emailData.size() == 0) {
                continue;
            }

            String data = emailData.get(emailData.size() - 1);
            // 我们需要的消息类型 [类，方法，参数，]
            // 反射执行
            QueueDTO queueDTO = new Gson().fromJson(data, QueueDTO.class);
            try {
                Class<?> c      = Class.forName(queueDTO.getRef());
                Method   method = c.getMethod(queueDTO.getMethod());
                method.invoke(null, queueDTO.getArgs());

            } catch (ClassNotFoundException e) {
                new Log().commonLog("pop_email", "ClassNotFoundException", "error");
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                new Log().commonLog("pop_email", "NoSuchMethodException", "error");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                new Log().commonLog("pop_email", "IllegalAccessException", "error");
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                new Log().commonLog("pop_email", "InvocationTargetException", "error");
                e.printStackTrace();
            }

            new Log().commonLog("pop_email", data, "info");
            try {
                sleep(20);
            } catch (InterruptedException e) {
                new Log().commonLog("pop_email", "InterruptedException", "error");
            }
        }
    }
}
