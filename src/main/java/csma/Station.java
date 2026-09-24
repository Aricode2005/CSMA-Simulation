package csma;

import csma.strategy.MacStrategy;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Station extends Thread {
    public int id;
    public String address;
    public int framesToSend;
    public Channel channel;
    public SimClock clock;
    public MacStrategy macStrategy;
    public boolean useCD;

    public long totalDelay = 0;
    public int successfulTransmissions = 0;
    
    private List<Frame> generatedFrames = new ArrayList<>();

    public static int TRANSMISSION_TIME = 20; 
    public static int PROPAGATION_DELAY = 10; 
    
    public int undetectedCollisions = 0; 
    
    private final Random random = new Random();

    public Station(int id, int frames, Channel channel, SimClock clock, MacStrategy macStrategy, boolean useCD) {
        this.id = id;
        this.address = "MAC-" + id;
        this.framesToSend = frames;
        this.channel = channel;
        this.clock = clock;
        this.macStrategy = macStrategy;
        this.useCD = useCD;
        loadPayloads();
    }
    
    private void loadPayloads() {
        try {
            String content = new String(Files.readAllBytes(Paths.get("input.txt"))).trim();
            if (content.isEmpty()) content = "Dummy data";
            
            int chunkSize = 46;
            int seq = 0;
            for (int i = 0; i < content.length(); i += chunkSize) {
                int end = Math.min(content.length(), i + chunkSize);
                byte[] payload = content.substring(i, end).getBytes();
                String dest = "MAC-" + random.nextInt(10); 
                generatedFrames.add(new Frame(this.address, dest, seq++, payload));
            }
        } catch (IOException e) {
            generatedFrames.add(new Frame(this.address, "MAC-0", 0, "Fallback data".getBytes()));
        }
    }
    
    public String getAddress() { return this.address; }

    public void receive(Frame frame) {
        if (frame.getDestinationAddress().equals(this.address)) {
           String payloadStr = new String(frame.getPayload()).replaceAll("[^\\x20-\\x7E]", "");
           log("RECEIVED", "Received Frame Seq " + frame.getSeqNo() + " from " + frame.getSourceAddress() + " | Data: " + payloadStr);
        }
    }

    public void log(String state, String msg) {
        if (SimClock.isLiveMode || SimClock.isVisualBatchMode) {
            String json = String.format("{\"type\": \"STATION\", \"id\": \"%s\", \"state\": \"%s\", \"msg\": \"%s\"}", 
                                        this.address, state, msg.replace("\"", "\\\""));
            System.out.println("[WS] " + json);
            System.out.flush();
        }
    }

    public boolean transmit(Frame frame) {
        if (useCD) {
            return transmitWithCD(frame);
        } else {
            return transmitWithoutCD(frame);
        }
    }

    @Override
    public void run() {
        clock.registerThread();
        try {
            int framesSent = 0;
            while (framesSent < framesToSend) {
                long arrivalTime = clock.getTick();
                boolean success = false;
                int k = 0; 
                
                Frame currentFrame = generatedFrames.get(framesSent % generatedFrames.size());
                log("SENSING", "Frame " + (framesSent+1) + "/" + framesToSend + " ready. Sensing channel...");

                while (!success) {
                    success = macStrategy.execute(this, channel, clock, currentFrame);

                    if (!success) {
                        k++; 
                        if (k > 15) {
                            log("DROPPED", "Frame " + currentFrame.getSeqNo() + " DROPPED after 15 failed attempts!");
                            break; 
                        }
                        randomBackoff(k);
                    }
                }
                
                long delay = clock.getTick() - arrivalTime;
                totalDelay += delay;
                
                if (success) {
                    successfulTransmissions++;
                }
                
                framesSent++;

                int nextArrival = (SimClock.isLiveMode || SimClock.isVisualBatchMode) ? (random.nextInt(10) + 5) : (random.nextInt(50) + 10);
                for (int i = 0; i < nextArrival; i++) {
                    clock.waitForNextTick();
                }
            }
            log("DONE", "All " + framesToSend + " frames processed. Station shutting down.");
        } finally {
            clock.deregisterThread();
        }
    }

    public boolean transmitWithoutCD(Frame frame) {
        log("TRANSMITTING", "Transmitting Frame " + frame.getSeqNo() + " (Without CD)");
        channel.startTx(frame);
        boolean collided = false;
        for (int i = 0; i < TRANSMISSION_TIME; i++) {
            clock.waitForNextTick();
            if (channel.isCollision()) {
                collided = true;
            }
        }
        channel.stopTx();
        if (collided) log("COLLISION", "Collision detected after full transmission.");
        else log("SUCCESS", "Frame " + frame.getSeqNo() + " sent successfully!");
        return !collided;
    }

    public boolean transmitWithCD(Frame frame) {
        log("TRANSMITTING", "Transmitting Frame " + frame.getSeqNo() + " (With CD)");
        channel.startTx(frame);
        boolean collided = false;
        boolean detected = false;
        
        // Simulating the vulnerability window.
        // Worst-case collision requires 2 * PROPAGATION_DELAY ticks to reach the sender.
        int ticksToDetect = 2 * PROPAGATION_DELAY - 1;
        
        for (int i = 0; i < TRANSMISSION_TIME; i++) {
            clock.waitForNextTick();
            
            // hardware simulation: Listen while talking (Read back voltage from the wire)
            // Jamming also counts as a collision signal
            if (channel.getTransmittingCount() > 1 || channel.isJamming()) {
                collided = true;
                // In a physical wire, the collision takes up to 2*Tp to reach us.
                // If our frame is too small (Tfr < 2*Tp), we might finish transmitting BEFORE the collision reaches us.
                // We model this constraint: you only detect it if your frame is long enough to cover the worst-case round trip.
                if (TRANSMISSION_TIME >= ticksToDetect) {
                    detected = true;
                    log("ABORTING", "Voltage spike / Jamming detected! Aborting.");
                } else {
                    detected = false;
                    // We don't log aborting because the station physically finished sending before the collision arrived, so it didn't know.
                }
                break; 
            }
        }
        
        if (collided) {
            if (detected) {
                channel.startJamming();
                log("JAMMING", "Broadcasting 48-bit JAM signal to ensure all stations detect collision.");
                for (int i = 0; i < 2; i++) {
                    clock.waitForNextTick();
                }
                channel.stopJamming();
            } else {
                log("ERROR", "Undetected Collision! Frame corrupted because Tfr < 2*Tp. Station failed to detect.");
                undetectedCollisions++;
                // The station *thinks* it succeeded, so it moves on.
                // But the channel knows it was corrupted.
                channel.stopTx();
                return true; 
            }
        } else {
            log("SUCCESS", "Frame " + frame.getSeqNo() + " sent successfully!");
        }
        
        channel.stopTx();
        return !collided;
    }

    private void randomBackoff(int attempt) {
        int maxBackoff = (int) Math.pow(2, Math.min(attempt, 10));
        int backoffSlots = random.nextInt(maxBackoff);
        log("BACKOFF", "Applying backoff. Waiting for " + backoffSlots + " slots.");
        for (int i = 0; i < backoffSlots; i++) {
            clock.waitForNextTick();
        }
    }
}
