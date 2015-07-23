package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;

/**
 * Created by Daniel on 2015-07-23.
 */
public class HBReceive extends Thread{
    Handler mHandler;
    int count = 0;

    HBReceive(Handler handler){
        mHandler = handler;
    }



    public void run(){
        while(true){

            //for checking heartbeat at every second.
            try {  Thread.sleep(1000);} catch (InterruptedException e) { }
            try {
                if (StateBuffer.HEARTBEATQUEUE.isEmpty()) {
                    count++;
                } else {
                    StateBuffer.RECEIEVEDATAQUEUE.poll();
                }

                if (count >= 30) {
                    Message msg = Message.obtain();
                    msg.what = 1;
                    mHandler.sendMessage(msg);
                    count = 0;
                }
            }catch (Exception e){
                Message msg = Message.obtain();
                msg.what = 255;
                mHandler.sendMessage(msg);

            }
        }
    }
}
