package csma;

import java.util.ArrayList;
import java.util.List;

public class Channel {
    private int transmittingCount = 0;
    private int txInCurrentPeriod = 0;
    private long collisions = 0;
    private SimClock clock;
    private long busyStartTime = -1;
    
    private List<Station> allStations = new ArrayList<>();
    private List<Frame> framesInTransit = new ArrayList<>();

    public Channel(SimClock clock) { 
        this.clock = clock; 
    }
    
    public void setStations(List<Station> stations) {
        this.allStations = stations;
    }

    public synchronized void startTx(Frame frame) {
        if (transmittingCount == 0) {
            txInCurrentPeriod = 0;
            busyStartTime = clock.getTick();
            framesInTransit.clear();
            if (SimClock.isLiveMode) {
                System.out.println("[WS] {\"type\": \"CHANNEL\", \"state\": \"BUSY\", \"msg\": \"Channel is now BUSY.\"}");
            }
        }
        transmittingCount++;
        txInCurrentPeriod++;
        framesInTransit.add(frame);
        
        if (txInCurrentPeriod == 2) {
            collisions++;
            if (SimClock.isLiveMode) {
                System.out.println("[WS] {\"type\": \"CHANNEL\", \"state\": \"COLLISION\", \"msg\": \"COLLISION DETECTED on the wire!\"}");
            }
        }
    }

    public synchronized void stopTx() {
        transmittingCount--;
        if (transmittingCount == 0) {
       
            if (txInCurrentPeriod == 1 && framesInTransit.size() == 1) {
                Frame successfulFrame = framesInTransit.get(0);
                broadcast(successfulFrame);
            }
            
            busyStartTime = -1;
            framesInTransit.clear();
            if (SimClock.isLiveMode) {
                System.out.println("[WS] {\"type\": \"CHANNEL\", \"state\": \"IDLE\", \"msg\": \"Channel is now IDLE.\"}");
            }
        }
    }
    
    private void broadcast(Frame frame) {
        for (Station s : allStations) {
            if (!s.getAddress().equals(frame.getSourceAddress())) {
                s.receive(frame);
            }
        }
    }

    public synchronized boolean isIdle() {
        if (transmittingCount == 0) return true;
        return clock.getTick() < busyStartTime + Station.PROPAGATION_DELAY;
    }

    public synchronized boolean isCollision() {
        return txInCurrentPeriod > 1;
    }
    
    public synchronized long getCollisionCount() { 
        return collisions; 
    }
}
