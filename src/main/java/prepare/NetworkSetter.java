package prepare;

import neuralnetwork.BackPropogation;
import neuralnetwork.BackPropogationGraph;
import neuralnetwork.Network;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.swing.*;
import java.awt.event.MouseListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Arc2D;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

/**
 * Created by Laurens on 11-6-2016.
 */
public class NetworkSetter {


    public static void main(String[] args) {
        Network network = new Network(HandChipsPlayer.INPUT_LAYER_SIZE, 1);
        network.insertLayer(150, 1);
        network.randomize();
        HandChipsPlayer handChipsPlayers = getHandChipsPlayers();
        BackPropogation[] backPropogations = new BackPropogation[1];
        for (int i = 0; i < backPropogations.length; i++) {
            backPropogations[i] = new BackPropogation(new Network(network), handChipsPlayers.getInput(), handChipsPlayers.getOutput());
        }
        BackPropogationGraph graph = BackPropogationGraph.createFrame("Poker networks");
        graph.add(backPropogations[0],"Threaded Poker network");
        backPropogations[0].setUseBoldDriver(true);
        for (int i = 0; i < 30000; i++) {
            int count = 0;
            for (BackPropogation backPropogation : backPropogations) {
                long time = System.currentTimeMillis();
                backPropogation.runEpochThreaded();
                backPropogation.addDeltas();
                graph.addMeasure(backPropogation);
                backPropogation.resetDeltas(-1);
                System.out.println("Time:" + (System.currentTimeMillis() - time));
                count++;
            }
        }

    }


    private static HandChipsPlayer getHandChipsPlayers() {
        JSONArray array = null;
        try {
            array = new JSONArray(HandChipsPlayer.readFile("hands.json"));
        } catch (IOException e) {
            e.printStackTrace();
        }
        HandChipsPlayer[] handsArray = new HandChipsPlayer[array.length()];
        for (int i = 0; i < array.length(); i++) {
            try {
                handsArray[i] = new HandChipsPlayer(array.getJSONObject(i));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        handsArray[0].merge(handsArray);
        return handsArray[0];
    }

}
