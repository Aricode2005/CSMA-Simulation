package csma.strategy;

import csma.Channel;
import csma.SimClock;
import csma.Station;
import csma.Frame;

public class NonPersistentStrategy implements MacStrategy {
    
    @Override
    public boolean execute(Station station, Channel channel, SimClock clock, Frame frame) {
        while (true) {
            if (channel.isIdle()) {
                return station.transmit(frame);
            } else {
                int randomSlots = (int) (Math.random() * 10) + 1;
                for (int i = 0; i < randomSlots; i++) {
                    clock.waitForNextTick();
                }
            }
        }
    }
}
