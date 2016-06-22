package prepare;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.Vector;

/**
 * Created by Laurens on 9-6-2016.
 */
public class HandChipsPlayer extends PokerHand {

    public static final double BIG_BLIND_DENOMINATOR = 80;
    public static final int PLAYER_CARDS_LENGTH = 4;
    public static final int COMMUNITY_CARDS_LENGTH = 10;
    public static final int GENERAL_INFO_LENGTH = 2;
    public static int INPUT_LAYER_SIZE = MAX_PLAYERS_AT_TABLE + (MAX_PLAYERS_AT_TABLE * BET_ROUNDS * ROUNDS_PER_BET_ROUND) + PLAYER_CARDS_LENGTH + COMMUNITY_CARDS_LENGTH + GENERAL_INFO_LENGTH;
    private  int bBpos;
    private double[] moneyOnTable = new double[MAX_PLAYERS_AT_TABLE];
    private double[] currentPurses = new double[MAX_PLAYERS_AT_TABLE];
    private double toCall;
    protected double[] startPurses = new double[MAX_PLAYERS_AT_TABLE];
    private double[] playerCards = new double[PLAYER_CARDS_LENGTH];
    private  int sBpos;
    private int tableRound;
    private double[] generalInfo = new double[GENERAL_INFO_LENGTH];
    private List<double[]> input = new Vector<>();
    private List<double[]> output = new Vector<>();

    public HandChipsPlayer(JSONObject pokerhand) {
        super(pokerhand);
        generalInfo[0] = heroPos;
        initPurses();
        toCall = bigBlind;
        sBpos = jsonPokerhand.getInt("SBpos");
        bBpos = jsonPokerhand.getInt("BBpos");
        decrementPurse(seatToPlayerPosition(bBpos), bigBlind);
        decrementPurse(seatToPlayerPosition(sBpos), bigBlind / 2);
        setCards(jsonPokerhand.getString("hand"), playerCards);
        setBets();
//        System.out.println(Arrays.toString(playerCards));
//        System.out.println(Arrays.toString(bets));
    }

    private void initPurses() {
        for (int i = 0; i < startPurses.length; i++) {
            startPurses[i] = 0d;
        }
        for (String seat : jsonPokerhand.getJSONObject("seats").keySet()) {
            int intSeat = seatToPlayerPosition(Integer.parseInt(seat));
            currentPurses[intSeat] = jsonPokerhand.getJSONObject("seats").getJSONObject(seat).getDouble("purse");
            decrementPurse(intSeat, ante);
            startPurses[intSeat] = currentPurses[intSeat] / bigBlind / BIG_BLIND_DENOMINATOR;
        }
    }

    private void setBets() {
        Arrays.fill(bets, 0d);//-1 indicates no bet. We are going to fill some holes. The rest should stay -1.
        setBets(0, jsonPokerhand.getJSONObject("preflop").getJSONObject("bets"), bBpos, jsonPokerhand.getJSONObject("preflop"));
        setBets(1, jsonPokerhand.getJSONObject("flop").getJSONObject("bets"), dealerPos, jsonPokerhand.getJSONObject("flop"));
        setBets(2, jsonPokerhand.getJSONObject("turn").getJSONObject("bets"), dealerPos, jsonPokerhand.getJSONObject("turn"));
        setBets(3, jsonPokerhand.getJSONObject("river").getJSONObject("bets"), dealerPos, jsonPokerhand.getJSONObject("river"));
    }

    private void setBets(int round, JSONObject bets, int fromSeat, JSONObject communityCards) {
//        System.out.println("Bet round: " + round);
        if (bets.keySet().size() == 0) {
            return;
        }
        if (round > 0) resetPlayerMoney();
        tableRound = 0;
        while (true) {
            fromSeat = nextPlayer(fromSeat, bets);
            if (fromSeat == -1) break;
            JSONObject action = bets.getJSONArray("" + fromSeat).getJSONObject(tableRound / tablesize);
            handlePlayerAction(round, fromSeat, tableRound / tablesize, action, communityCards.has("cards") ? communityCards.getString("cards") : "");
            tableRound++;
        }
    }

    private void resetPlayerMoney() {
        for (int i = 0; i < moneyOnTable.length; i++) {
            moneyOnTable[i] = 0d;
        }
        toCall = 0d;
    }

    public double[][] getInput() {
        return input.toArray(new double[input.size()][]);
    }

    public double[][] getOutput() {
        return output.toArray(new double[output.size()][]);
    }

    private void handlePlayerAction(int round, int fromSeat, int betRound, JSONObject action, String communityCards) {
        double bet = 0d;
        int seatIndex = seatToPlayerPosition(fromSeat);
        String actionString = action.getString("action");
        bet = calculateAddedMoney(action, bet, seatIndex, actionString);
//        System.out.println("\tSeat " + fromSeat + " made a bet of " + bet);
        int arrayPosition = seatToArrayPosition(fromSeat, round, tableRound / tablesize);
        handleHero(fromSeat, communityCards, bet);
        this.bets[arrayPosition] = bet / bigBlind;
    }

    private void handleHero(int fromSeat, String communityCards, double bet) {
        if (fromSeat == heroPos) {
            double[] doubleCommunityCards = new double[COMMUNITY_CARDS_LENGTH];
            setCards(communityCards, doubleCommunityCards);
            double[] toPresent = merge(startPurses, bets, playerCards, doubleCommunityCards, generalInfo);
            input.add(toPresent);
            double valueBetweenOneAndZero = Math.min(bet / bigBlind / BIG_BLIND_DENOMINATOR, 1);
            //Now we map the value between -1 and 1
            output.add(new double[]{valueBetweenOneAndZero});
        }
    }

    private double calculateAddedMoney(JSONObject action, double bet, int seatIndex, String actionString) {
        if(actionString.equals("FOLD")){
            bet = 0d;
        } else if (actionString.equals("CHECK")) {
            bet = 0d;
        } else if (actionString.equals("CALL")) {
            double amount = toCall - moneyOnTable[seatIndex];
            bet = amount;
            decrementPurse(seatIndex, amount);
            moneyOnTable[seatIndex] = toCall;
        } else if (actionString.equals("ALL-IN")) {
            double addition = currentPurses[seatIndex];
            bet = handleAllin(seatIndex, addition);
        } else if (actionString.equals("BET") || actionString.equals("RAISE")) {
            double addition = action.getDouble("amount");
            bet = handleRaise(seatIndex, addition);
        }
        return bet;
    }

    private double handleRaise(int seatIndex, double addition) {
        toCall = addition;
        return decrementPurse(seatIndex, addition-moneyOnTable[seatIndex]);
    }

    private double handleAllin(int seatIndex, double addition) {
        double debt = toCall - moneyOnTable[seatIndex];
        toCall += (addition - debt);
        return decrementPurse(seatIndex, addition);
    }


    private int nextPlayer(int from, JSONObject actions) {
        Set<String> keys = actions.keySet();
        while (true) {
            from = next(from);
            if (actions.has("" + from) && actions.getJSONArray("" + from).length() > (tableRound / tablesize)) {
                return from;
            } else if (actions.has("" + from)) {
                return -1;
            }
            tableRound++;
        }
    }

    private int next(int from) {
        int table = (from + 1) % (tablesize);
        return table == 0 ? table + tablesize : table;
    }

    private double decrementPurse(int index, double amount) {
        double purse = currentPurses[index];
        if (amount >= purse) {
            currentPurses[index] = 0d;
            moneyOnTable[index] += purse;
            return purse;
        } else {
            currentPurses[index] -= amount;
            moneyOnTable[index] += amount;
            return amount;
        }
    }

    private void merge(HandChipsPlayer other) {
        input.addAll(other.input);
        output.addAll(other.output);
    }

    public void merge(HandChipsPlayer... others) {
        for (HandChipsPlayer other : others) {
            if(other!=this&&other!=null)merge(other);
        }
    }


    public static String readFile(String name) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine()) != null) {
            builder.append(line);
            builder.append("\n");
        }
        reader.close();
        return builder.toString();
    }

    private double[] merge(double[]... arrays) {
        int length = 0;
        for (double[] array : arrays) {
            length += array.length;
        }
        double[] toReturn = new double[length];
        int index = 0;
        for (double[] array : arrays) {
            for (double v : array) {
                toReturn[index++] = v;
            }
        }
        return toReturn;
    }

    private static double getSuit(String card) {
        String suit = card.substring(card.length() - 1).toLowerCase();
        String suits = "cdsh";
        double index = suits.indexOf(suit);
        return index / (suits.length() - 1);
    }

    private static double getValue(String card) {
        String cards = "2345678910JQKA";
        String firstpart = card.startsWith("1") ? card.substring(0, 2) : card.substring(0, 1);
        double index = cards.indexOf(firstpart);
        return index / (cards.length() - 1);
    }

    private void setCards(String cards, double[] putHere) {
        int index = 0;
        Arrays.fill(putHere, -1d);
        for (int i = 0; i < cards.length(); ) {
            int length = cards.charAt(i) == '1' ? 3 : 2;
            String card = cards.substring(i, i + length);
            putHere[index++] = getValue(card);
            putHere[index++] = getSuit(card);
            i += length;
        }
    }


}
