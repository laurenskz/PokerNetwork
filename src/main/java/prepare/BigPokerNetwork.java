package prepare;

import dbconnection.MongoDBJDBC;
import neuralnetwork.BackPropogation;
import neuralnetwork.BackPropogationGraph;
import neuralnetwork.Network;

import java.util.Arrays;

/**
 * Created by Laurens on 16-6-2016.
 */
public class BigPokerNetwork {

    private Network network;
    private BackPropogation backPropogation;
    private BackPropogationGraph backPropogationGraph;

    public BigPokerNetwork(String name) {
        network = createNetwork();
        initialize(name);
    }

    public BigPokerNetwork(String name, Network network) {
        this.network = network;
        initialize(name);
    }

    private void initialize(String name) {
        MongoDBJDBC database = new MongoDBJDBC(name == null ? "Downloads" : name);
//        for (int i = 0; i < database.getAllHands().getOutput().length; i++) {
//            System.out.println(Arrays.toString(database.getAllHands().getInput()[i]));
//            System.out.println(Arrays.toString(database.getAllHands().getOutput()[i]));
//        }
        backPropogation = new BackPropogation(network, database.getAllHands().getInput(), database.getAllHands().getOutput());
        backPropogation.setUseBoldDriver(true);
//        backPropogation.setMinLearningRate(Double.MIN_VALUE);
        backPropogation.setLearningRate(0.0003);
        backPropogationGraph = BackPropogationGraph.createFrame("Big poker network");
        backPropogationGraph.add(backPropogation, "The network");
    }

    public void runIteration(int i) {
        backPropogation.runEpochThreaded();
        backPropogation.addDeltas();
        backPropogationGraph.addMeasure(backPropogation);
        System.out.println(String.format("Error is %f", backPropogation.getError()));
        backPropogation.resetDeltas(-1);
        network.writeTo("Pokernetwork.json");
    }

    public void runIterations(int count) {
        for (int i = 0; i < count; i++) {
            if(i%1000==0)System.out.println(String.format("Starting iteration %d", i));
            runIteration(i);
            if(i%1000==0)System.out.println(String.format("Completed iteration %d", i));
        }
    }

    private static Network createNetwork() {
        Network network = new Network(HandChipsPlayer.INPUT_LAYER_SIZE, 1);
        network.insertLayer(60, 1);
        network.insertLayer(HandChipsPlayer.INPUT_LAYER_SIZE, 1);
//        network.insertLayer(14, 1);
//        network.insertLayer(14, 1);
//        network.insertLayer(14, 1);
        network.randomize();
        return network;
    }

    public static void main(String[] args) {

        BigPokerNetwork bigPokerNetwork;
        if (args.length > 1) {
            bigPokerNetwork = new BigPokerNetwork(args[0],Network.fromFile(args[1]));
        } else {
            bigPokerNetwork = new BigPokerNetwork(args.length == 0 ? null : args[0],Network.fromFile("Pokernetwork.json"));
        }
        while (true) {
            bigPokerNetwork.runIteration(0);
        }
    }
}
