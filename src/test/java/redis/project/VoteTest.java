package redis.project;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/09/30 09:53
 */
public class VoteTest {

    Vote vote = new Vote();

    @Test
    public void articleVote() {
        vote.articleVote(Const.AID, Const.UID);
    }
}