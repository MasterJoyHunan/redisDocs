package redis.project;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/09/30 15:26
 */
public class UserTest {
    public User user = new User();
    @Test
    public void checkToken() {
//        user.checkToken();
    }

    @Test
    public void updateToken() {
        for (int i = 0; i < 5; i++) {
            for (int j = 5; j < 11; j++) {
                user.updateToken(i + "token", i, j);
            }
        }
    }
}