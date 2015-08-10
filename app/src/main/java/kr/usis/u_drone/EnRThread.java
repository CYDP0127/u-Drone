package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;

/**
 * Created by Daniel on 2015-08-07.
 */
public class EnRThread extends Thread {
    Handler mHandler;
    double elasped_time = 0;
    double remain_time;
    double current_voltage;
    int temp = 0 ;

    EnRThread(Handler handler, double current_voltage) {
        mHandler = handler;
        this.current_voltage = current_voltage;
    }

    public void run() {
        try {
            Thread.sleep(3000);
        } catch (Exception e) {
        }
        if ((((current_voltage - 21.6) * 600) / 3.3) < 0)
            remain_time = -1 * (((current_voltage - 21.6) * 600) / 3.3);
        else
            remain_time = (((current_voltage - 21.6) * 600) / 3.3);
        remain_time /= 60;

        while (true) {
            remain_time-=0.01;
            elasped_time+=0.01;

            temp = (int)remain_time*100;
            if((remain_time*100 - temp)>60)
                remain_time -= 0.4;

            temp = (int)elasped_time *100;
            if((elasped_time*100 -temp)>60)
                elasped_time += 0.4;


            Message msg = Message.obtain();
            msg.what = 2;
            msg.arg1 = (int)(elasped_time*100);
            msg.arg2 = (int)(remain_time*100);
            mHandler.sendMessage(msg);

            try {
                Thread.sleep(1000);
            } catch (Exception e) {
            }
        }
    }

}
