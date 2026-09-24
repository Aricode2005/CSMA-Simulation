package csma;

import csma.strategy.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class Simulator {

    static class Result {
        long collisions;
        long undetectedCollisions;
        double avgDelay;
        double throughput;

        public Result(long c, long uc, double d, double t) {
            collisions = c;
            undetectedCollisions = uc;
            avgDelay = d;
            throughput = t;
        }
    }

    public static Result runExperiment(MacStrategy strategy, boolean useCD, int N, int framesPerStation) {
        SimClock clock = new SimClock();
        Channel channel = new Channel(clock);
        List<Station> stations = new ArrayList<>();
        
        for (int i = 0; i < N; i++) {
            stations.add(new Station(i, framesPerStation, channel, clock, strategy, useCD));
        }
        
        channel.setStations(stations);

        for (Station s : stations) {
            s.start();
        }

        for (Station s : stations) {
            try {
                s.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        long totalTime = clock.getTick();
        int totalSuccessful = 0;
        long totalDelay = 0;
        long totalUndetected = 0;
        
        for (Station s : stations) {
            totalSuccessful += s.successfulTransmissions;
            totalDelay += s.totalDelay;
            totalUndetected += s.undetectedCollisions;
        }

        // Frames that had undetected collisions are actually corrupt, so they aren't true successes.
        int validSuccess = totalSuccessful - (int)totalUndetected;
        if (validSuccess < 0) validSuccess = 0;

        double avgDelay = totalSuccessful > 0 ? (double) totalDelay / totalSuccessful : 0;
        double throughput = totalTime > 0 ? ((double) validSuccess * Station.TRANSMISSION_TIME) / totalTime : 0;

        return new Result(channel.getCollisionCount(), totalUndetected, avgDelay, throughput);
    }
    
    private static int readFramesFromFile(String filename) {
        try {
            String content = new String(Files.readAllBytes(Paths.get(filename))).trim();
            return Integer.parseInt(content);
        } catch (Exception e) {
            System.err.println("Could not read " + filename + ". Defaulting to 100 frames.");
            return 100;
        }
    }

    public static void main(String[] args) {
        if (args.length > 0) {
            if (args[0].equals("LIVE")) {
                runLiveSimulation(args);
                return;
            } else if (args[0].equals("EXP1")) {
                SimClock.isVisualBatchMode = true;
                runExperiment1();
                return;
            } else if (args[0].equals("EXP2")) {
                SimClock.isVisualBatchMode = true;
                runExperiment2();
                return;
            } else if (args[0].equals("EXP3")) {
                SimClock.isVisualBatchMode = true;
                runExperiment3();
                return;
            }
        }

        System.out.println("Starting CSMA Simulation with LLD Strategy Pattern...");
        runExperiment1();
        runExperiment2();
        runExperiment3();
        System.out.println("\nSimulation completed. Results saved to CSV files.");
    }

    private static void setupVisualBatch(int N, boolean isFirst) {
        if (isFirst) {
            SimClock.isVisualBatchMode = true;
            System.out.println("[WS] {\"type\": \"SETUP\", \"numStations\": " + N + "}");
            System.out.flush();
        } else {
            SimClock.isVisualBatchMode = false;
        }
    }

    private static void runExperiment1() {
        int frames = readFramesFromFile("workload.txt");
        System.out.println("Running Experiment 1: p-Persistent (N=10), varying p...");
        try (PrintWriter out = new PrintWriter(new FileWriter("experiment1_p_persistent.csv"))) {
            out.println("p,Collisions,AvgDelay,Throughput");
            int fixedN = 10;
            double[] pValues = {0.01, 0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0};
            
            boolean isFirst = true;
            for (double p : pValues) {
                setupVisualBatch(fixedN, isFirst);
                isFirst = false;
                Result res = runExperiment(new PPersistentStrategy(p), false, fixedN, frames);
                out.printf("%.2f,%d,%.2f,%.4f\n", p, res.collisions, res.avgDelay, res.throughput);
                System.out.printf("p=%.2f -> Collisions: %d, Avg Delay: %.2f, Throughput: %.4f\n", p, res.collisions, res.avgDelay, res.throughput);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runExperiment2() {
        int frames = readFramesFromFile("workload.txt");
        System.out.println("\nRunning Experiment 2: All schemes, varying N...");
        MacStrategy[] strategies = {
            new NonPersistentStrategy(),
            new OnePersistentStrategy(),
            new PPersistentStrategy(0.1), 
            new CsmaCdStrategy() 
        };
        boolean[] useCDFlags = {false, false, false, false}; 
        String[] strategyNames = {"Non-Persistent", "1-Persistent", "p-Persistent", "CSMA/CD"};
        int[] nValues = {2, 5, 10, 15, 20, 25, 30};

        try (PrintWriter out = new PrintWriter(new FileWriter("experiment2_varying_N.csv"))) {
            out.println("Strategy,N,Collisions,AvgDelay,Throughput");
            boolean isFirst = true;
            for (int i = 0; i < strategies.length; i++) {
                System.out.println("Testing strategy: " + strategyNames[i]);
                for (int n : nValues) {
                    setupVisualBatch(n, isFirst);
                    isFirst = false;
                    Result res = runExperiment(strategies[i], useCDFlags[i], n, frames);
                    out.printf("%s,%d,%d,%.2f,%.4f\n", strategyNames[i], n, res.collisions, res.avgDelay, res.throughput);
                    System.out.printf("  N=%d -> Collisions: %d, Avg Delay: %.2f, Throughput: %.4f\n", n, res.collisions, res.avgDelay, res.throughput);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void runExperiment3() {
        int frames = readFramesFromFile("workload.txt");
        System.out.println("\nRunning Experiment 3: Tfr vs Tp constraint...");
        try (PrintWriter out = new PrintWriter(new FileWriter("experiment3_tfr_vs_tp.csv"))) {
            out.println("Tfr,UndetectedCollisions,Throughput");
            Station.PROPAGATION_DELAY = 10;
            int[] tfrValues = {5, 10, 15, 18, 20, 22, 25, 30}; // 2*Tp = 20
            MacStrategy csmaCd = new CsmaCdStrategy();
            
            boolean isFirst = true;
            for (int tfr : tfrValues) {
                setupVisualBatch(10, isFirst);
                isFirst = false;
                Station.TRANSMISSION_TIME = tfr;
                Result res = runExperiment(csmaCd, true, 10, frames);
                out.printf("%d,%d,%.4f\n", tfr, res.undetectedCollisions, res.throughput);
                System.out.printf("  Tfr=%d (2*Tp=20) -> Undetected Collisions: %d, Throughput: %.4f\n", tfr, res.undetectedCollisions, res.throughput);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private static void runLiveSimulation(String[] args) {
        SimClock.isLiveMode = true;
        String strategyName = args[1];
        boolean useCD = Boolean.parseBoolean(args[2]);
        double pValue = Double.parseDouble(args[3]);
        int numStations = Integer.parseInt(args[4]);
        
        MacStrategy strategy;
        if (strategyName.equals("NON")) {
            strategy = new NonPersistentStrategy();
        } else if (strategyName.equals("P")) {
            strategy = new PPersistentStrategy(pValue);
        } else {
            strategy = new OnePersistentStrategy();
        }

        // Run experiment (only 5 frames per station so it doesn't take forever visually)
        Result res = runExperiment(strategy, useCD, numStations, 5);
        String metrics = String.format("Avg Delay: %.2f slots | Throughput: %.4f", res.avgDelay, res.throughput);
        System.out.println("[WS] {\"type\": \"CHANNEL\", \"state\": \"FINISHED\", \"msg\": \"Live simulation completed! " + metrics + "\"}");
        System.out.flush();
        System.exit(0);
    }
}
