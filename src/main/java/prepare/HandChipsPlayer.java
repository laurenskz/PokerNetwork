package prepare;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Set;

/**
 * Created by Laurens on 9-6-2016.
 */
public class HandChipsPlayer extends PokerHand {

    private final int bBpos;
    private double[] moneyOnTable = new double[MAX_PLAYERS_AT_TABLE];
    private double[] currentPurses = new double[MAX_PLAYERS_AT_TABLE];
    private  double toCall;
    protected double[] startPurses = new double[MAX_PLAYERS_AT_TABLE];
    private final int sBpos;
    private int tableRound;

    public HandChipsPlayer(JSONObject pokerhand) {
        super(pokerhand);
        initPurses();
        toCall = bigBlind;
        sBpos = jsonPokerhand.getInt("SBpos");
        bBpos = jsonPokerhand.getInt("BBpos");
        decrementPurse(seatToPlayerPosition(bBpos),bigBlind);
        decrementPurse(seatToPlayerPosition(sBpos),bigBlind/2);
        setBets();
        System.out.println(Arrays.toString(bets));
    }

    private void initPurses() {
        for (int i = 0; i < startPurses.length; i++) {
            startPurses[i] = -1d;
        }
        for (String seat : jsonPokerhand.getJSONObject("seats").keySet()) {
            int intSeat = seatToPlayerPosition(Integer.parseInt(seat));
            currentPurses[intSeat] = jsonPokerhand.getJSONObject("seats").getJSONObject(seat).getDouble("purse");
            decrementPurse(intSeat,ante);
            startPurses[intSeat] = currentPurses[intSeat]/bigBlind;
        }
    }

    private void setBets(){
        Arrays.fill(bets, -1d);//-1 indicates no bet. We are going to fill some holes. The rest should stay -1.
        setBets(0,jsonPokerhand.getJSONObject("preflop").getJSONObject("bets"),bBpos);
        setBets(1,jsonPokerhand.getJSONObject("flop").getJSONObject("bets"),dealerPos);
        setBets(2,jsonPokerhand.getJSONObject("turn").getJSONObject("bets"),dealerPos);
        setBets(3,jsonPokerhand.getJSONObject("river").getJSONObject("bets"),dealerPos);
    }

    private void setBets(int round, JSONObject bets, int fromSeat){
        System.out.println("Bet round: " + round);
        if (bets.keySet().size() == 0) {
            return;
        }
        if(round>0)resetPlayerMoney();
        tableRound = 0;
        while (true) {
            fromSeat = nextPlayer(fromSeat, bets);
            if(fromSeat == -1)break;
            JSONObject action = bets.getJSONArray("" + fromSeat).getJSONObject(tableRound/tablesize);
            handlePlayerAction(round, fromSeat, tableRound/tablesize, action);
            tableRound++;
        }
    }

    private void resetPlayerMoney() {
        for (int i = 0; i < moneyOnTable.length; i++) {
            moneyOnTable[i] = 0d;
        }
        toCall = 0d;
    }

    private void handlePlayerAction(int round, int fromSeat, int betRound, JSONObject action) {
        double bet = 0d;
        int seatIndex = seatToPlayerPosition(fromSeat);
        String actionString = action.getString("action");
        if (actionString.equals("CHECK") || actionString.equals("FOLD")) {
            bet = 0d;
        } else if (actionString.equals("CALL")) {
            double amount = toCall - moneyOnTable[seatIndex];
            bet = amount;
            decrementPurse(seatIndex, amount);
            moneyOnTable[seatIndex] = toCall;
        } else if (actionString.equals("ALL-IN")) {
            double addition = bet = currentPurses[seatIndex];
            handleRaise(seatIndex, addition);
        } else if (actionString.equals("BET") || actionString.equals("RAISE")) {
            double addition = bet = action.getDouble("amount");
            handleRaise(seatIndex, addition);
        }
        System.out.println("\tSeat " + fromSeat + " made a bet of " + bet);
        int arrayPosition = seatToArrayPosition(fromSeat, round, tableRound / tablesize);
        System.out.println("Array position = " + arrayPosition);
        this.bets[arrayPosition] = bet/bigBlind;
    }

    private void handleRaise(int seatIndex, double addition) {
        double debt = toCall - moneyOnTable[seatIndex];
        toCall += (addition - debt);
        moneyOnTable[seatIndex] += addition;
        decrementPurse(seatIndex,addition);
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

    private void decrementPurse(int index, double amount) {
        double purse = currentPurses[index];
        if (amount >= purse) {
            currentPurses[index] = 0d;
            moneyOnTable[index] += purse;
        }else{
            currentPurses[index] -= amount;
            moneyOnTable[index] += amount;
        }
    }

    public static void main(String[] args) {
        try {
            new HandChipsPlayer(new JSONObject(readFile("hand.json")));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String readFile(String name) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
        StringBuilder builder = new StringBuilder();
        String line = null;
        while ((line = reader.readLine())!=null){
            builder.append(line);
            builder.append("\n");
        }
        reader.close();
        return builder.toString();
    }


}
