package kr.usis.u_drone;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.usis.serial.act.DeviceListActivity;
import kr.usis.serial.util.HexDump;
import kr.usis.serial.util.SerialInputOutputManager;

public class MainActivity extends ActionBarActivity {
    public  TextView textview;

    boolean DisconnectedFlag = false;
    DeviceListActivity dla;

    DisplayThread dThread;
    BackThread mThread;

    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    //Handler for getting start Input Thread. - Daniel
   Handler mHandler = new Handler(){
        public void handleMessage(Message msg){
            if(msg.what == 0){
                startIoManager();
            }
        }

    };

    Handler dHandler = new Handler(){
        public void handleMessage(Message msg){
            Bundle bundle = msg.getData();
            String string = bundle.getString("test");
            textview.append(string);
            textview.invalidate();
        }

    };


    //handler for displaying recieved data. - Daniel
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {

                }
                @Override
                public void onNewData(final String data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };

/*    //handler for displaying recieved data. - Daniel
    private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {

                }
                @Override
                public void onNewData(final MAVLinkMessage data) {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };*/


    //display input data
        private void updateReceivedData(String data) {
        //final String message = HexDump.dumpHexString(data);

        textview.append(data);
        textview.append(" .");
        textview.invalidate();
    }

/*    private void updateReceivedData(MAVLinkMessage data) {
        //final String message = HexDump.dumpHexString(data);
        textview.append("SysId=" + data.sysId + " CompId=" + data.componentId + " seq=" + data.sequence + " " );
        textview.append("  ");
        textview.invalidate();
    }*/


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

        textview = (TextView)findViewById(R.id.textView);

        final Button ConnectButton = (Button) findViewById(R.id.ConnectButton);

        ConnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                connect();
            }
        });

        final Button DisconnectButton = (Button) findViewById(R.id.DisconnectButton);

        DisconnectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                disconnect();
            }
        });


    }


    public void connect() {
        if (!DisconnectedFlag) {
            Toast.makeText(this, "Connection Established", Toast.LENGTH_SHORT).show();
            dla = new DeviceListActivity(this);
            dla.getUSBService();
            dla.refreshDeviceList();

            mThread = new BackThread(mHandler);
            mThread.setDaemon(true);
            mThread.start();

            dThread = new DisplayThread(dHandler);
            dThread.setDaemon(true);
            dThread.start();

      //      new displayQueue().execute();

            DisconnectedFlag = true;
        }
    }

    //disconnect button event
    public void disconnect() {
        Toast.makeText(this, "Connection Destroyed", Toast.LENGTH_SHORT).show();
        dla = null;
        DisconnectedFlag = false;
        mThread = null;
        textview.setText("");
        mSerialIoManager.stop();
    }


    /*private class displayQueue extends AsyncTask<Void,MAVLinkMessage,Void>{

        @Override
        protected Void doInBackground(Void... params) {
                while(true) {

                    if(StateBuffer.RECEIEVEDATAQUEUE.isEmpty()){
                        try{Thread.sleep(1000);}catch (InterruptedException e) {}
                    }
                    else {
                        MAVLinkMessage msg = StateBuffer.RECEIEVEDATAQUEUE.poll();
                        textview.append("poll");
                        publishProgress(msg);
                    }

                    //need to make terminating this thread function...
                }
        }

        @Override
        protected void onProgressUpdate(MAVLinkMessage... progress){
                    textview.setText(progress[0].toString());
                    textview.invalidate();
        }
    }*/


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

//Thread for checking connection. - Daniel
class DisplayThread extends Thread{
    Handler mHandler;


    DisplayThread(Handler handler){
        mHandler = handler;
    }

    public void run(){

        Message msgs = mHandler.obtainMessage();
        Bundle bundle = new Bundle();
        int i=0;
        while(10>=i++){
            if(StateBuffer.RECEIEVEDATAQUEUE.isEmpty()){
                try{Thread.sleep(1000);}catch (InterruptedException e) {;}
            }
            else{
                MAVLinkMessage msg = StateBuffer.RECEIEVEDATAQUEUE.poll();
                if(msg==null){
                    bundle.putString("test","fail");
                }else {
                    bundle.putString("test", msg.toString());
                }
                    msgs.setData(bundle);
                    mHandler.sendMessage(msgs);

            }
        }
    }
}

