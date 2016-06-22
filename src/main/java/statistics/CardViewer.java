package statistics;

import dbconnection.MongoDBJDBC;
import neuralnetwork.Network;
import org.json.JSONArray;
import org.json.JSONObject;
import prepare.HandChipsPlayer;
import sun.nio.ch.Net;
import utils.ThreadPool;

import java.io.IOException;
import java.util.*;

/**
 * Created by Laurens on 21-6-2016.
 */
public class CardViewer {

    public static final int AMOUNT_OF_HANDS = 5;
    private JSONObject input;
    private Network neuralNetwork;
    private JSONObject output;
    private String[] players;

    public CardViewer(JSONObject input) {
        this.input = input;
        JSONArray options = input.getJSONArray("Options");
        output = new JSONObject();
        output.put("playercards", new JSONObject());
        neuralNetwork = Network.fromFile("Pokernetwork.json");
        setPlayers(options);
        doWork();
    }

    private void setPlayers(JSONArray options) {
        for (int i = 0; i < options.length(); i++) {
            if (options.getJSONObject(i).getString("option").equals("viewcards")) {
                JSONArray players = options.getJSONObject(i).getJSONArray("players");
                this.players = new String[players.length()];
                for (int j = 0; j < players.length(); j++) {
                    this.players[j] = players.getString(j);
                }
            }
        }
    }

    private List<String> getCards() {
        List<String> toReturn = new ArrayList<>(52);
        for (String card : MongoDBJDBC.cards) {
            for (String suit : "HDCS".split("")) {
                toReturn.add(card + suit);
            }
        }
        return toReturn;
    }

    private void doWork() {
        for (String player : players) {
            List<String> cards = getCards();
            cards.removeAll(getCommunityCards(input));
            JSONObject jsonObject = new JSONObject(input, JSONObject.getNames(input));
            List<Pair> playerCards = new ArrayList<>(AMOUNT_OF_HANDS);
            for (int i = 0; i < cards.size(); i++) {
                for (int j = i+1; j < cards.size(); j++) {
                    String starthand = cards.get(i) + cards.get(j);
                    jsonObject.put("hero", Integer.parseInt(player));
                    jsonObject.put("hand", starthand);
                    HandChipsPlayer handChipsPlayer = new HandChipsPlayer(jsonObject);
                    double error = 0d;
                    for (int k = 0; k < handChipsPlayer.getInput().length; k++) {
                        double[] output = neuralNetwork.evaluate(handChipsPlayer.getInput()[k]);
                        error += Math.abs(output[0] - handChipsPlayer.getOutput()[k][0]);
                    }
                    change(new Pair(starthand,error),playerCards,AMOUNT_OF_HANDS);
                }
            }
            for (Pair playerCard : playerCards) {
                output.getJSONObject("playercards").append(player, playerCard.card);
            }
        }
    }

    private static void change(Pair newPair, List<Pair> existing, int maxSize) {
        if (existing.size() < maxSize) {
            existing.add(newPair);
            if(existing.size()==maxSize)Collections.sort(existing);
            return;
        }
        if (newPair.rating < existing.get(existing.size() - 1).rating) {
            existing.add(newPair);
            Collections.sort(existing);
            existing.remove(existing.size() - 1);
        }
    }

    private List<String> getCommunityCards(JSONObject input) {
        for (String state : "river, turn, flop".split(", ")) {
            if (input.getJSONObject(state).has("cards")) {
                String cards = input.getJSONObject(state).getString("cards");
                return parseCards(cards);
            }
        }
        return new ArrayList<>(0);
    }

    private static class Pair implements Comparable<Pair>{
        String card;
        Double rating;

        public Pair(String card, Double rating) {
            this.card = card;
            this.rating = rating;
        }

        @Override
        public int compareTo(Pair o) {
            return rating.compareTo(o.rating);
        }

        @Override
        public String toString() {
            return "Pair{" +
                    "card='" + card + '\'' +
                    ", rating=" + rating +
                    '}';
        }
    }

    private List<String> parseCards(String cards){
        List<String> cardList = new Vector<>();
        for (int i = 0; i < cards.length(); ) {
            int length = cards.charAt(i) == '1' ? 3 : 2;
            String card = cards.substring(i, i + length);
            cardList.add(card);
            i += length;
        }
        return cardList;
    }

    public JSONObject getOutput() {
        return output;
    }

    public static JSONObject getPossibleCards(JSONObject situation) {
        return new CardViewer(situation).getOutput();
    }

    public static void main(String[] args) {
        try {
            JSONObject situation = new JSONObject(HandChipsPlayer.readFile("hand.json"));
            JSONObject option = new JSONObject();
            option.accumulate("option", "viewcards");
            option.append("players", "2");
            option.append("players", "3");
            option.append("players", "5");
            situation.append("Options", option);
            System.out.println(getPossibleCards(situation).toString(2));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
