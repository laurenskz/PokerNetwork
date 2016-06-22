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

    protected JSONObject jsonPokerhand;
    protected int dealerPos, heroPos;
    protected double bigBlind;
    protected double ante;
    //This array represents all betting behaviour. Index 0 represents person UTG (first after dealer) in FLOP ( will never happen, but you get the idea).
    protected double[] bets = new double[MAX_PLAYERS_AT_TABLE * BET_ROUNDS * ROUNDS_PER_BET_ROUND];
    protected int tablesize;

    public PokerHand(JSONObject pokerhand) {
        this.jsonPokerhand = pokerhand;
        initFromJson();
    }

    private void initFromJson() {
        dealerPos = jsonPokerhand.getInt("dealer");
        heroPos = jsonPokerhand.getInt("hero");
        bigBlind = jsonPokerhand.getDouble("BB");
        tablesize = MAX_PLAYERS_AT_TABLE;
    }



    /**
     * This method return the index of the betposition.
     *
     * @param seat
     * @return
     */
    protected int seatToPlayerPosition(int seat) {
        int seatsBeforeDealer = seatsBeforeDealer(seat);
        return MAX_PLAYERS_AT_TABLE - 1 - seatsBeforeDealer;
    }

    protected int seatsBeforeDealer(int seat) {
        return Math.floorMod(dealerPos - seat, tablesize);
    }

    /**
     * @param seat
     * @param betRound   preflop = 0, flop = 1, turn = 2, river = 3
     * @param tableRound The round at the table.
     * @return
     */
    protected int seatToArrayPosition(int seat, int betRound, int tableRound) {
        if (betRound == 0 && seatsBeforeDealer(seat) >= tablesize - 2)
            ++tableRound;//Big blind and small blind have different bet position preflop
        return seatToPlayerPosition(seat) + (betRound * ROUNDS_PER_BET_ROUND + tableRound) * MAX_PLAYERS_AT_TABLE;

    }





}
