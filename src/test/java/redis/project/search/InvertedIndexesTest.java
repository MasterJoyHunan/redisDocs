package redis.project.search;

import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * @author joy
 * @time 2019/10/24 18:47
 */
public class InvertedIndexesTest {

    @Test
    public void indexDocument() {
        String v1 = "Once there were two mice. They were friends. One mouse lived in the country; the other mouse lived in the city. After many years the Country mouse saw the City mouse; he said, \"Do come and see me at my house in the country.\" So the City mouse went. The City mouse said, \"This food is not good, and your house is not good. Why do you live in a hole in the field? You should come and live in the city. You would live in a nice house made of stone. You would have nice food to eat. You must come and see me at my house in the city.\" The Country mouse went to the house of the City mouse. It was a very good house. Nice food was set ready for them to eat. But just as they began to eat they heard a great noise. The City mouse cried, \" Run! Run! The cat is coming!\" They ran away quickly and hid. After some time they came out. When they came out, the Country mouse said, \"I do not like living in the city. I like living in my hole in the field. For it is nicer to be poor and happy, than to be rich and afraid.\"";
        new InvertedIndexes().indexDocument(8 + "", v1);

        String v2 = "\"I am quite ready,\" answered the Emperor. \"Do my new clothes fit well?\" asked he, turning himself round again before the looking glass, in order that he might appear to be examining his handsome suit.\n" +
                "The lords of the bedchamber, who were to carry his Majesty's train felt about on the ground, as if they were lifting up the ends of the mantle; and pretended to be carrying something; for they would by no means betray anything like simplicity, or unfitness for their office.\n" +
                "So now the Emperor walked under his high canopy in the midst of the procession, through the streets of his capital; and all the people standing by, and those at the windows, cried out, \"Oh! How beautiful are our Emperor's new clothes! What a magnificent train there is to the mantle; and how gracefully the scarf hangs!\" in short, no one would allow that he could not see these much-admired clothes; because, in doing so, he would have declared himself either a simpleton or unfit for his office. Certainly, none of the Emperor's various suits, had ever made so great an impression, as these invisible ones.\n";
        new InvertedIndexes().indexDocument(9 + "", v2);

        String v3 = "Many years ago, there was an Emperor, who was so excessively fond of new clothes, that he spent all his money in dress. He did not trouble himself in the least about his soldiers; nor did he care to go either to the theatre or the chase, except for the opportunities then afforded him for displaying his new clothes. He had a different suit for each hour of the day; and as of any other king or emperor, one is accustomed to say, \"he is sitting in council,\"it was always said of him, \"The Emperor is sitting in his wardrobe.\"\n" +
                "Time passed merrily in the large town which was his capital; strangers arrived every day at the court. One day, two rogues, calling themselves weavers, made their appearance. They gave out that they knew how to weave stuffs of the most beautiful colors and elaborate patterns, the clothes manufactured from which should have the wonderful property of remaining invisible to everyone who was unfit for the office he held, or who was extraordinarily simple in character.\n";
        new InvertedIndexes().indexDocument(10 + "", v3);
    }


    @Test
    public void parseQuery() {
        Map<String, Object> res      = new InvertedIndexes().parseQuery("+one +two +love -bobo +sex alics");
        List<List<String>>  search   = (List<List<String>>) res.get("all");
        Set<String>         unwanted = (Set<String>) res.get("unwanted");
        System.out.println(search);
        System.out.println(unwanted);
    }

    @Test
    public void parseAndSearch() {
        String res = new InvertedIndexes().parseAndSearch("+again +two +love -ago +sex alics");
        System.out.println(res);
    }


    @Test
    public void intersect() {
    }

    @Test
    public void union() {
    }

    @Test
    public void difference() {
    }
}