package csma.strategy;

import csma.Channel;
import csma.SimClock;
import csma.Station;
import csma.Frame;
import java.util.Random;

public class PPersistentStrategy implements MacStrategy {
    private final double p;
    private final Random random = new Random();

    public PPersistentStrategy(double p) {
        this.p = p;
    }

    @Override
    public boolean execute(Station station, Channel channel, SimClock clock, Frame frame) {
        while (!channel.isIdle()) {
            clock.waitForNextTick();
        }
        
        while (true) {
            if (random.nextDouble() <= p) {
                return station.transmit(frame);
            } else {
                clock.waitForNextTick();
                
                if (!channel.isIdle()) {
                    return false; 
                }
            }
        }
    }
}
