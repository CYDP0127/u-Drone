package kr.usis.u_drone;

import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;


import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.usis.serial.act.DeviceListActivity;
import kr.usis.serial.util.HexDump;
import kr.usis.serial.util.SerialInputOutputManager;

public class MainActivity extends ActionBarActivity {
    TextView textview;

    BackThread mThread;

    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    //Handler for getting start Input Thread. - Daniel
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg){
            if(msg.what == 0){
                startIoManager();
            }
        }

    };

    //handler for displaying recieved data. - Daniel
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {

                }
                @Override
                public void onNewData(final byte[] data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };

    // display message on sce
    private void updateReceivedData(byte[] data) {
        final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        textview.append(message);
        textview.invalidate();
    }

    private void startIoManager() {
        if (StateBuffer.CONNECTION!= null) {
            mSerialIoManager = new SerialInputOutputManager(StateBuffer.CONNECTION, mListener);
            mExecutor.submit(mSerialIoManager);
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    //button event
    public void onClick(View v) {
        DeviceListActivity dla = new DeviceListActivity(this);
        dla.getUSBService();
        dla.refreshDeviceList();

        textview = (TextView) findViewById(R.id.textView);
        textview.setText("testtest");

        mThread = new BackThread(mHandler);
        mThread.setDaemon(true);
        mThread.start();

    }
}

//Thread for checking connection. - Daniel
class BackThread extends Thread{
    Handler mHandler;


    BackThread(Handler handler){
        mHandler = handler;
    }


    private boolean isConnected(){
        return StateBuffer.CREATEDCONNECTION;
    }

    public void run(){

        while(!isConnected()){
            try{Thread.sleep(1000);}catch (InterruptedException e) {;}
        }

        Message msg = Message.obtain();
        msg.what = 0;
        mHandler.sendMessage(msg);

        }
}
