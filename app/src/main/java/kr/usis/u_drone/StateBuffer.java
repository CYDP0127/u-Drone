package kr.usis.u_drone;

import org.mavlink.messages.MAVLinkMessage;

import java.util.LinkedList;
import java.util.Queue;

import kr.usis.serial.driver.UsbSerialPort;

/**
 * Created by Daniel on 2015-07-16.
 */

// StateBuffer checks connection state, usb address, que for receiving data from reading
public class StateBuffer {

    public static boolean CREATEDCONNECTION = false;

    public static UsbSerialPort CONNECTION = null;

    public static Queue<MAVLinkMessage> RECEIEVEDATAQUEUE = new LinkedList<>();
    public static Queue<MAVLinkMessage> HEARTBEATQUEUE = new LinkedList<>();
    public static int sequence =0;

    public static synchronized int increaseSequence(){
        return (sequence = (StateBuffer.sequence++) % 256);
    }


}
