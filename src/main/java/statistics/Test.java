package statistics;

import dbconnection.MongoDBJDBC;
import neuralnetwork.Network;
import org.json.JSONObject;
import prepare.HandChipsPlayer;
import prepare.NetworkSetter;
import prepare.PokerHand;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by Laurens on 18-6-2016.
 */
public class Test {


    private static HandChipsPlayer handChipsPlayer = null;
    private static JSONObject pokerhand;

    public static void main(String[] args) {
        try {
            pokerhand = new JSONObject(HandChipsPlayer.readFile("hand.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        Network network = Network.fromFile("Pokernetwork.json");
        for (int i = 0; i < MongoDBJDBC.cards.length; i++) {
            for (int j = i; j < MongoDBJDBC.cards.length; j++) {
                String card = MongoDBJDBC.cards[i] + "H" + MongoDBJDBC.cards[j] + "D";
                pokerhand.put("hand", card);
                System.out.println(card);
                handChipsPlayer = new HandChipsPlayer(pokerhand);
                System.out.println(Arrays.toString(network.evaluate(handChipsPlayer.getInput()[0])));
            }
        }

    }
}
