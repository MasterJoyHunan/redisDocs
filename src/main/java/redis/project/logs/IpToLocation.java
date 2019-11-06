package redis.project.logs;


import com.google.gson.Gson;
import lombok.Getter;
import lombok.Setter;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Set;

/**
 * @author joy
 * @time 2019/10/15 09:46
 */
public class IpToLocation {

    /**
     * 根据IP获取所在的城市 过期的数据，无法使用
     *
     * @param ip IP
     * @deprecated
     */
    public void findCityByIp(String ip) {
        int         score   = ip2Score(ip);
        Jedis       redis   = RedisUtil.getRedis();
        Set<String> results = redis.zrevrangeByScore("IP_TO_CITY:", score, 0, 0, 1);
        System.out.println(score);
        if (results.size() == 0) {
            return;
        }

        String cityId = results.iterator().next();
        cityId = cityId.substring(0, cityId.indexOf('_'));
        String[] res = new Gson().fromJson(redis.hget("CITY_ID_TO_CITY:", cityId), String[].class);
        for (String str : res) {
            System.out.println(str);
        }
    }

    /**
     * 将IP转为整数分值
     *
     * @param ip IP
     * @return int
     */
    public int ip2Score(String ip) {
        int      score = 0;
        String[] ips   = ip.split("\\.");
        for (String v : ips) {
            score = score * 256 + Integer.parseInt(v, 10);
        }
        return score;
    }


    public void importIps(String fileName) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            int            count  = 0;
            reader.readLine();
            reader.readLine();
            String      line;
            Jedis       redis = RedisUtil.getRedis();
            Transaction trans = redis.multi();
            while ((line = reader.readLine()) != null) {

                String[] lines = line.replace("\"", "").split(",");

                trans.zadd("IP_TO_CITY:", Double.parseDouble(lines[0]), lines[2] + "_" + count);
                if (count % 10 == 0) {
                    trans.exec();
                    trans = redis.multi();
                }
                count++;
            }
            trans.exec();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void importCities(String fileName) {
        try {
            Gson           gson   = new Gson();
            BufferedReader reader = new BufferedReader(new FileReader(fileName));
            int            count  = 0;
            reader.readLine();
            reader.readLine();
            String      line;
            Jedis       redis = RedisUtil.getRedis();
            Transaction trans = redis.multi();
            while ((line = reader.readLine()) != null) {
                String[] lines  = line.replace("\"", "").split(",");
                String   cityID = lines[0];
                // 国家
                String country = lines[1];
                // 区域
                String region = lines[2];
                // 城市
                String city = lines[3];

                trans.hset("CITY_ID_TO_CITY:", lines[0], gson.toJson(new String[]{country, region, city}));
                if (count % 100 == 0) {
                    trans.exec();
                    trans = redis.multi();
                }
                count++;
            }
            trans.exec();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
