package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_COMPONENT;
import org.mavlink.messages.ardupilotmega.msg_command_long;
import org.mavlink.messages.ardupilotmega.msg_heartbeat;

import java.nio.ByteBuffer;

/**
 * Created by �ֿ��(Daniel) on 2015-07-23.
 */

//Heartbeat �۽� ������ 1Hz (1�ʿ� 1��)
public class HBSend extends Thread {

    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);

    public void run() {

        byte[] buff = null;
        msg_heartbeat hb = new msg_heartbeat(1, 1);
//        hb.mavlink_version = 3;

        while (true) {
            //for checking heartbeat at every second.
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            try {
                hb.sequence = StateBuffer.increaseSequence();
                mWriteBuffer.put(hb.encode());
                synchronized (mWriteBuffer) {
                    int len = mWriteBuffer.position();
                    if (len > 0) {
                        buff = new byte[17];
                        mWriteBuffer.rewind();
                        mWriteBuffer.get(buff, 0, len);
                        mWriteBuffer.clear();
                    }
                }
                if (buff != null) {
                    StateBuffer.CONNECTION.write(buff, 1000);
                }
            } catch (Exception e) {

            }


        }
    }
}
