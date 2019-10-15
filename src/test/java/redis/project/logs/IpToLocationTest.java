package redis.project.logs;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/15 10:10
 */
public class IpToLocationTest {

    @Test
    public void importIps() {
        new IpToLocation().importIps("E:\\document\\ip-mapping\\GeoLiteCity-Blocks.csv");
    }

    @Test
    public void importCities() {
        new IpToLocation().importCities("E:\\document\\ip-mapping\\GeoLiteCity-Location.csv");
    }


    @Test
    public void findCityByIp() {
//        new IpToLocation().findCityByIp("55.24.47.35");
    }


}