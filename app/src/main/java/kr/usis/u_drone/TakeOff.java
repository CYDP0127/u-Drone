/*
package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;

import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.ardupilotmega.msg_command_long;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_override;

import java.nio.ByteBuffer;

*/
/**
 * Created by Daniel on 2015-07-23.
 *//*

public class TakeOff extends Thread {
    Handler mHandler;
    Handler mHandler;
    int count = 0;

    TakeOff(Handler handler) {
        mHandler = handler;
    }

    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);


    public void run() {
        while (true) {

            try {
                byte[] buff = null;
                msg_command_long arm = new msg_command_long(1,1);
                arm.command = MAV_CMD.MAV_CMD_NAV_TAKEOFF;

                arm.param1 = 0;
                arm.param2 = 0;
                arm.param3 = 0;
                arm.param4 = 0;
                arm.param5 = 0;
                arm.param6 = 0;
                arm.param7 = 1;

                arm.sequence = StateBuffer.increaseSequence();
                arm.target_system = 1;
                arm.target_component = 1;
                arm.confirmation = 0;
               */
/* byte[] buff = null;
                msg_rc_channels_override arm = new msg_rc_channels_override(1, 1);
                arm.chan1_raw = 1000;  //roll
                arm.chan2_raw = 1000;  //pitch
                arm.chan3_raw = 1100; //throttle
                arm.chan4_raw = 1000;   //yaw
                arm.chan5_raw = 0;
                arm.chan6_raw = 0;
                arm.chan7_raw = 0;
                arm.chan8_raw = 0;
                arm.sequence = StateBuffer.increaseSequence();
                arm.target_system = 1;
                arm.target_component = 1;*//*

                mWriteBuffer.put(arm.encode());

                synchronized (mWriteBuffer) {
                    int len = mWriteBuffer.position();
                    if (len > 0) {
                        buff = new byte[26];
                        mWriteBuffer.rewind();
                        mWriteBuffer.get(buff, 0, len);
                        mWriteBuffer.clear();
                    }
                }
                if (buff != null) {
                    StateBuffer.CONNECTION.write(buff, 2000);
                }

            } catch (Exception e) {
                Message msg = Message.obtain();
                msg.what = 200;
                mHandler.sendMessage(msg);


            }
        }
    }
}
*/
