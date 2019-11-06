package redis.project.bit;

import redis.RedisUtil;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;
import redis.clients.jedis.ZParams;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * 打包存储二进制位和字节
 *
 * @author joy
 * @time 2019/11/04 16:21
 */
public class Code {

    private static final String[]              COUNTRY = new String[]{"USA", "CN"};
    private static final Map<String, String[]> STATES  = new HashMap<>();

    static {
        STATES.put("CN", "AB BC MB NB NL NS NT NU ON PE QC SK YT".split(" "));
        STATES.put("USA", (
                "AA AE AK AL AP AR AS AZ CA CO CT DC DE FL FM GA GU HI IA ID IL IN " +
                        "KS KY LA MA MD ME MH MI MN MO MP MS MT NC ND NE NH NJ NM NV NY OH " +
                        "OK OR PA PR PW RI SC SD TN TX UT VA VI VT WA WI WV WY").split(" "));
    }

    /**
     * 获取省市区code
     *
     * @param country
     * @param state
     * @return
     */
    public String getCode(String country, String state) {
        int cindex = Arrays.binarySearch(COUNTRY, country);
        if (cindex < 0 || !country.equals(COUNTRY[cindex])) {
            cindex = 0;
        } else {
            cindex++;
        }
        int sindex = 0;
        if (STATES.containsKey(country) && state != null) {
            sindex = Arrays.binarySearch(STATES.get(country), state);
        }
        sindex = sindex < 0 ? 0 : sindex++;
        return new String(new char[]{(char) cindex, (char) sindex});
    }


    private int USERS_PER_SHARD = 1000000;

    /**
     * 将用户对应的省市区设置进去
     *
     * @param uid
     * @param country
     * @param state
     */
    public void setLocation(int uid, String country, String state) {
        String code     = getCode(country, state);
        int    shardId  = uid / USERS_PER_SHARD;
        int    position = uid % USERS_PER_SHARD;
        int    offset   = position * 2;

        Pipeline pipe = RedisUtil.getRedis().pipelined();
        pipe.setrange("LOCATION:" + shardId, offset, code);
        String tkey = UUID.randomUUID().toString();
        pipe.zadd(tkey, uid, "max");
        pipe.zunionstore("LOCATION:MAX", new ZParams().aggregate(ZParams.Aggregate.MAX), tkey, "LOCATION:MAX");
        pipe.del(tkey);
        pipe.sync();
    }

    /**
     * 聚合所有用户数据
     */
    public void aggregateLocation() {
        Map<String, Long>              country = new HashMap<>();
        Map<String, Map<String, Long>> state   = new HashMap<>();
        Jedis                          redis   = RedisUtil.getRedis();

        long   maxId    = redis.zscore("LOCATION:MAX", "max").longValue();
        long   maxBlock = maxId;
        byte[] buffer   = new byte[(int) Math.pow(2, 17)];
        for (int shardId = 0; shardId <= maxBlock; shardId++) {
            InputStream in = new RedisStream("location:" + shardId);
            try {
                int read = 0;
                // 每次读取128kb
                while ((read = in.read(buffer, 0, buffer.length)) != -1) {
                    for (int offset = 0; offset < read - 1; offset += 2) {
                        String code = new String(buffer, offset, 2);
                        updateAggregates(country, state, code);
                    }
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
            } finally {
                try {
                    in.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }


    /**
     * 聚合特定用户数据
     */
    public void aggregateLocationList(int[] uids) {
        Map<String, Long>              country = new HashMap<>();
        Map<String, Map<String, Long>> state   = new HashMap<>();
        Pipeline                       pipe    = RedisUtil.getRedis().pipelined();
        for (int i = 0; i < uids.length; i++) {
            long shardId  = uids[i] / USERS_PER_SHARD;
            int  position = uids[i] % USERS_PER_SHARD;
            int  offset   = position * 2;
            pipe.substr("LOCATION:" + shardId, position, offset);

            // 每1000次执行一次
            if ((i + 1) % 1000 == 0) {
                updateAggregates(country, state, pipe.syncAndReturnAll());
            }
        }
        updateAggregates(country, state, pipe.syncAndReturnAll());
    }


    /**
     * 聚合数据
     *
     * @param countries
     * @param states
     * @param code
     */
    public void updateAggregates(Map<String, Long> countries, Map<String, Map<String, Long>> states, String code) {
        if (code.length() != 2) {
            return;
        }
        int countryIdx = (int) code.charAt(0) - 1;
        int stateIdx   = (int) code.charAt(1) - 1;
        if (countryIdx < 0 || countryIdx >= COUNTRY.length) {
            return;
        }
        String country    = COUNTRY[countryIdx];
        Long   countryAgg = countries.get(country);
        if (countryAgg == null) {
            countryAgg = 0L;
        }
        // 国家
        countries.put(country, countryAgg + 1);
        if (!STATES.containsKey(country)) {
            return;
        }
        if (stateIdx < 0 || stateIdx >= STATES.get(country).length) {
            return;
        }
        String state = STATES.get(country)[stateIdx];

        // 城市聚合
        Map<String, Long> stateAggs = states.get(country);
        if (stateAggs == null) {
            stateAggs = new HashMap<>();
            states.put(country, stateAggs);
        }
        Long stateAgg = stateAggs.get(state);
        if (stateAgg == null) {
            stateAgg = 0L;
        }
        stateAggs.put(state, stateAgg + 1);
    }


    public void updateAggregates(Map<String, Long> countries, Map<String, Map<String, Long>> states, List<Object> codes) {
        for (Object code : codes) {
            updateAggregates(countries, states, (String) code);
        }
    }
}
