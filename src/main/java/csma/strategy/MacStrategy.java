package csma.strategy;

import csma.Channel;
import csma.SimClock;
import csma.Station;
import csma.Frame;

public interface MacStrategy {
    boolean execute(Station station, Channel channel, SimClock clock, Frame frame);
}
