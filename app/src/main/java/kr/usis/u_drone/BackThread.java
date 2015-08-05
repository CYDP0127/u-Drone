package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;

/**
 * Created by 최용득(Daniel) on 2015-07-23.
 */

//커넥션 확인 쓰레드
//Thread for checking connection. - Daniel
class BackThread extends Thread {
    Handler mHandler;

    BackThread(Handler handler) {
        mHandler = handler;
    }

    private boolean isConnected() {
        return StateBuffer.CREATEDCONNECTION;
    }

    public void run() {

        while (!isConnected()) {
            try {Thread.sleep(1000);} catch (InterruptedException e) {}
        }
        Message msg = Message.obtain();
        msg.what = 0;
        mHandler.sendMessage(msg);

    }
}
