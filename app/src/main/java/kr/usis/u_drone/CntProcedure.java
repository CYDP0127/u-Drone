package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;

import org.mavlink.messages.ardupilotmega.msg_heartbeat;
import org.mavlink.messages.ardupilotmega.msg_param_request_list;
import org.mavlink.messages.ardupilotmega.msg_request_data_stream;

import java.nio.ByteBuffer;

/**
 * Created by Daniel on 2015-08-05.
 */
class CntProcedure extends Thread {
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);
    byte[] buff = null;

    public void putdata(byte[] data) {
        mWriteBuffer.put(data);

    }

    public void sleep(int ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
        }
    }

    public void senddata() {
        try {
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

    public void sendRDS(int rate, int id){
        msg_request_data_stream rds = new msg_request_data_stream(1,1);
        rds.start_stop = 1;
        rds.req_message_rate = rate;
        rds.req_stream_id = id;
        rds.target_system = 1;
        rds.target_component = 1;
        rds.sequence = StateBuffer.increaseSequence();
        try {
            putdata(rds.encode());
        } catch (Exception e) {
        }
        senddata();
        sleep(10);
    }


    public void run() {
        //Send HeartBeat 9 times
        msg_heartbeat hb = new msg_heartbeat(1, 1);
        for (int i = 0; i <= 7; i++) {
            //for checking heartbeat at every second.
            hb.sequence = StateBuffer.increaseSequence();
            try {
                putdata(hb.encode());
            } catch (Exception e) {
            }
            senddata();
            sleep(10);
        }

        //send paran request list 1 time
        msg_param_request_list prl = new msg_param_request_list(1, 1);
        prl.sequence = StateBuffer.increaseSequence();
        prl.target_system = 1;
        prl.target_component = 1;
        try {
            putdata(prl.encode());
        } catch (Exception e) {
        }
        senddata();
        sleep(10);

        sendRDS(0x02, 0x00);
        sendRDS(0x02, 0x00);
        sendRDS(0x03, 0x06);
        sendRDS(0x03, 0x06);
        sendRDS(0x10, 0x10);
        sendRDS(0x10, 0x10);
        sendRDS(0x10, 0x11);
        sendRDS(0x10, 0x11);
        sendRDS(0x02, 0x12);
        sendRDS(0x02, 0x12);
        sendRDS(0x02, 0x01);
        sendRDS(0x02, 0x01);
        sendRDS(0x02, 0x03);
        sendRDS(0x02,0x03);
    }
}