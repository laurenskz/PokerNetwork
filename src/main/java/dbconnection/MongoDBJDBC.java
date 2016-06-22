package dbconnection;
import com.mongodb.MongoClient;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;
import prepare.HandChipsPlayer;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

public class MongoDBJDBC {

    public static final String[] cards = "A, K, Q, J, 10, 9, 8, 7, 6, 5, 4, 3, 2".split(", ");
    private static final BasicDBObject[] allCardCombinations;
    public static final int AMOUNT_OF_HANDS_PER_COMBINATION = 1000;

    static {
        allCardCombinations = iterateOverCards();
    }

    private MongoClient mongoClient;
    private MongoDatabase downloads;
    private MongoCollection<Document> replays;
    private HandChipsPlayer allHands;

    public MongoDBJDBC(String dbName) {
        getHands(dbName);
    }

    public void getHands(String dbName) {
        try{
            initializeDatabaseConnection(dbName);
            allHands = createAllHands();
        }catch(Exception e){
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
        }
    }

    public HandChipsPlayer getAllHands() {
        return allHands;
    }

    private HandChipsPlayer createAllHands() {
        HandChipsPlayer[] allHands = new HandChipsPlayer[allCardCombinations.length* AMOUNT_OF_HANDS_PER_COMBINATION];//Create this locally. We want to free the hands as soon as possible
        int index = 0;
        int iteration = 0;
        for (BasicDBObject combination : allCardCombinations) {
            FindIterable<Document> documents = replays.find(combination).limit(2000);//Leave a little room for error
            int count = 0;
            int errors = 0;
            for (Document document : documents) {
                if(count==AMOUNT_OF_HANDS_PER_COMBINATION)break;//We have 1000 hands, this should be enough
                try {
                    JSONObject jsonObject = new JSONObject(document);
                    HandChipsPlayer handChipsPlayer = new HandChipsPlayer(jsonObject);
                    allHands[index++] = handChipsPlayer;
                    ++count;
                } catch (JSONException e) {
                    ++errors;
                }
            }
            System.out.println(iteration);
            ++iteration;
        }
        return createMain(allHands);
    }

    private HandChipsPlayer createMain(HandChipsPlayer[] allHands) {
        int first;
        for (first = 0; first < allHands.length; first++) {//Get the first hand that is not null
            if (allHands[first] != null) break;
        }
        HandChipsPlayer main = allHands[first];
        main.merge(allHands);
        return main;
    }

    private void initializeDatabaseConnection(String dbName) {
        mongoClient = new MongoClient( "localhost" , 27017 );
        downloads = mongoClient.getDatabase(dbName);
        replays = downloads.getCollection("replays");
    }

    private static BasicDBObject[] iterateOverCards() {
        int index = 0;
        BasicDBObject[] allCardCombinations = new BasicDBObject[91];
        for (int i = 0; i < cards.length; i++) {
            for (int j = i; j < cards.length; j++) {
                String regex = cards[i] + "." + cards[j] + ".";
                allCardCombinations[index++] = new BasicDBObject("hand", Pattern.compile(regex));
            }
        }
        return allCardCombinations;
    }

}