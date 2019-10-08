package redis.project;

import org.junit.Test;
import redis.RedisUtil;
import redis.clients.jedis.Jedis;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/09/30 09:37
 */
public class ArticleTest {

    Article article = new Article();

    @Test
    public void add() {
        for (int i = 1; i < 100; i++) {
            article.add(i);
        }
    }

    @Test
    public void getPage() {
        for (int i = 1; i < 10; i++) {
            article.getPage("article:article_score", i);
        }
    }

    @Test
    public void addToGroup() {
        String[] s = {"java", "php"};
        for (int i = 1; i < 5; i++) {
            article.addToGroup(i, s);
        }
    }


    @Test
    public void removeToGroup() {
        String[] s = {"java"};
        article.removeToGroup(1, s);
    }

    @Test
    public void getGroup() {
        for (int i = 1; i < 4; i++) {
            article.getGroup("java", i);
        }
    }
}