package kr.usis.u_drone;

import org.mavlink.messages.MAVLinkMessage;

import java.util.LinkedList;
import java.util.Queue;

import kr.usis.serial.driver.UsbSerialPort;

/**
 * Created by 최용득(Daniel) on 2015-07-16.
 */

// StateBuffer checks connection state, usb address, que for receiving data from reading
public class StateBuffer {

    //드론과 모바일이 연결되었는지 확인하는 flag
    public static boolean CREATEDCONNECTION = false;

    //연결된 인스턴스 저장
    public static UsbSerialPort CONNECTION = null;

    //데이터 수신 큐
    public static Queue<MAVLinkMessage> RECEIEVEDATAQUEUE = new LinkedList<>();

    //heartbeat전용 큐
    public static Queue<MAVLinkMessage> HEARTBEATQUEUE = new LinkedList<>();

    //데이터 송신 큐
    public static Queue <byte [] > BufferStorage = new LinkedList<>();

    //체널값 송신에 사용되는 flag
    public static boolean flagThread_ch_send_Run = false;

    //패킷에 들어가는 sequence number. 매번 사용할때마다 1씩 자동 증가 (최대 FF)
    public static int sequence = 0;

    public static synchronized int increaseSequence(){
        StateBuffer.sequence++;
        sequence = StateBuffer.sequence % 256;
        return sequence;
}


}
