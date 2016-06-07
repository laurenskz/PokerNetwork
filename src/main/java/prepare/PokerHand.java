package prepare;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Created by Laurens on 7-6-2016.
 */
public class PokerHand {

    public static final int MAX_PLAYERS_AT_TABLE = 10;
    public static final int BET_ROUNDS = 4;//Preflop, flop, turn, river
    public static final int ROUNDS_PER_BET_ROUND = 4;

    private JSONObject jsonPokerhand;
    private int dealerPos, heroPos;
    private double bigBlind;
    private double ante;
    private double[] startPurses = new double[MAX_PLAYERS_AT_TABLE];
    private double[] currentPurses = new double[MAX_PLAYERS_AT_TABLE];
    //This array represents all betting behaviour. Index 0 represents person UTG (first after dealer) in FLOP ( will never happen, but you get the idea).
    private double[] bets = new double[MAX_PLAYERS_AT_TABLE * BET_ROUNDS * ROUNDS_PER_BET_ROUND];
    private int tablesize;

    public PokerHand(JSONObject pokerhand) {
        this.jsonPokerhand = pokerhand;
        initPurses();
        initFromJson();
    }

    private void initFromJson() {
        dealerPos = jsonPokerhand.getInt("dealer");
        heroPos = jsonPokerhand.getInt("hero");
        bigBlind = jsonPokerhand.getDouble("BB");
        tablesize = jsonPokerhand.getInt("players");
    }

    private void setBets() throws Exception {
        Arrays.fill(bets, -1d);//-1 indicates no bet. We are going to fill some holes. The rest should stay -1.
        setBets(0,jsonPokerhand.getJSONObject("preflop").getJSONObject("bets"));
        setBets(1,jsonPokerhand.getJSONObject("flop").getJSONObject("bets"));
        setBets(2,jsonPokerhand.getJSONObject("turn").getJSONObject("bets"));
        setBets(3,jsonPokerhand.getJSONObject("river").getJSONObject("bets"));
    }

    private void setBets(int round, JSONObject bets) throws Exception{
        for (String playerSeat : bets.keySet()) {
            int intSeat = Integer.parseInt(playerSeat);
            JSONArray playerBets = bets.getJSONArray(playerSeat);
            handleRaises(round, intSeat, playerBets);
        }
    }

    private void handleRaises(int round, int intSeat, JSONArray playerBets) throws Exception {
        for (int i = 0; i < playerBets.length(); i++) {
            double bet;
            if (playerBets.getJSONObject(i).has("amount")) {
                double amount = playerBets.getJSONObject(i).getDouble("amount");
                bet = amount / bigBlind;
            } else {
                String action = playerBets.getJSONObject(i).getString("action");
                if (action.equals("CHECK") || action.equals("FOLD")) {
                    bet = 0d;
                }else{
                    throw new Exception("Hand is not in a valid format, CALL also needs an amount");
                }
            }
            this.bets[seatToArrayPosition(intSeat, round, i)] = bet;
        }
    }

    /**
     * This method return the index of the betposition.
     *
     * @param seat
     * @return
     */
    private int seatToPlayerPosition(int seat) {
        int seatsBeforeDealer = seatsBeforeDealer(seat);
        return MAX_PLAYERS_AT_TABLE - 1 - seatsBeforeDealer;
    }

    private int seatsBeforeDealer(int seat) {
        return (dealerPos - seat) % tablesize;
    }

    /**
     * @param seat
     * @param betRound   preflop = 0, flop = 1, turn = 2, river = 3
     * @param tableRound The round at the table.
     * @return
     */
    private int seatToArrayPosition(int seat, int betRound, int tableRound) {
        if (betRound == 0 && seatsBeforeDealer(seat) >= tablesize - 2)
            ++betRound;//Big blind and small blind have different bet position preflop
        return seatToPlayerPosition(seat) + (betRound * ROUNDS_PER_BET_ROUND + tableRound) * MAX_PLAYERS_AT_TABLE;

    }


    private void initPurses() {
        for (int i = 0; i < startPurses.length; i++) {
            startPurses[i] = -1d;
        }
        for (String seat : jsonPokerhand.getJSONObject("seats").keySet()) {
            int intSeat = seatToPlayerPosition(Integer.parseInt(seat));
            startPurses[intSeat] = jsonPokerhand.getJSONObject("seats").getDouble("purse")/bigBlind;
        }
        currentPurses = Arrays.copyOf(startPurses, startPurses.length);
    }


}
