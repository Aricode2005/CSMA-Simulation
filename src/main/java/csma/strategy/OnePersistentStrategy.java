package csma.strategy;

import csma.Channel;
import csma.SimClock;
import csma.Station;
import csma.Frame;

public class OnePersistentStrategy implements MacStrategy {

    @Override
    public boolean execute(Station station, Channel channel, SimClock clock, Frame frame) {
        while (!channel.isIdle()) {
            clock.waitForNextTick();
        }
        return station.transmit(frame);
    }
}
