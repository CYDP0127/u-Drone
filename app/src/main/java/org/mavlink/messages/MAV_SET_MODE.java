package org.mavlink.messages;

/**
 * Created by Daniel on 2015-07-28.
 */
public interface MAV_SET_MODE {
    int STABILIZE = 0;
    int ALTHOLD = 2;
    int AUTO = 3;
    int GUIDED = 4;
    int LOITER = 5;
    int RTL = 6;
    int CIRCLE = 7;
    int POSITION = 8;
    int LAND = 9;
}
