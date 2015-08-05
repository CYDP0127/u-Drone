package kr.usis.u_drone;

import org.mavlink.messages.MAVLinkMessage;

import java.util.LinkedList;
import java.util.Queue;

import kr.usis.serial.driver.UsbSerialPort;

/**
 * Created by �ֿ��(Daniel) on 2015-07-16.
 */

// StateBuffer checks connection state, usb address, que for receiving data from reading
public class StateBuffer {

    //��а� ������� ����Ǿ����� Ȯ���ϴ� flag
    public static boolean CREATEDCONNECTION = false;

    //����� �ν��Ͻ� ����
    public static UsbSerialPort CONNECTION = null;

    //������ ���� ť
    public static Queue<MAVLinkMessage> RECEIEVEDATAQUEUE = new LinkedList<>();

    //heartbeat���� ť
    public static Queue<MAVLinkMessage> HEARTBEATQUEUE = new LinkedList<>();

    //������ �۽� ť
    public static Queue <byte [] > BufferStorage = new LinkedList<>();

    //ü�ΰ� �۽ſ� ���Ǵ� flag
    public static boolean flagThread_ch_send_Run = false;

    //��Ŷ�� ���� sequence number. �Ź� ����Ҷ����� 1�� �ڵ� ���� (�ִ� FF)
    public static int sequence = 0;

    public static synchronized int increaseSequence(){
        StateBuffer.sequence++;
        sequence = StateBuffer.sequence % 256;
        return sequence;
}


}
