package kr.usis.u_drone;

import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.ardupilotmega.msg_attitude;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_raw;
import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.lang.reflect.Array;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.usis.serial.act.DeviceListActivity;
import kr.usis.serial.util.HexDump;
import kr.usis.serial.util.SerialInputOutputManager;

public class MainActivity extends FragmentActivity {

    TextView text_roll;
    TextView text_yaw;
    TextView text_pitch;
    TextView text_altitude;
    TextView[] textView = new TextView[30];


    boolean DisconnectedFlag = false;
    DeviceListActivity dla;

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

    //display input data
        private void updateReceivedData(String data) {
/*        textview.append(data);
        textview.append(" ");
        textview.invalidate();*/
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
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        textView[0] = (TextView)findViewById(R.id.textView15);     //PITCH
        textView[1] = (TextView)findViewById(R.id.textView17);    //ROLL
        textView[2] = (TextView)findViewById(R.id.textView18);     //YAW

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

            new Dequeue().execute();

            DisconnectedFlag = true;
        }
    }

    //disconnect button event
    public void disconnect() {
        Toast.makeText(this, "Connection Destroyed", Toast.LENGTH_SHORT).show();
        dla = null;
        DisconnectedFlag = false;
        mThread = null;


    }

    public class Dequeue extends AsyncTask<Void,String,Void>{

        @Override
        protected void onProgressUpdate(String... strings) {
            for(int i =0;i<3;i++){
                textView[i].setText(strings[i]);
            }
            textView[0].setText(strings[0]);
        }


        @Override
        protected Void doInBackground(Void... arg0){
            int counter = 0;
            String[] valuse = new String[100];
            while(true) {
                if (StateBuffer.RECEIEVEDATAQUEUE.isEmpty()) {
                    try {
                        counter++;
                        Thread.sleep(100);
                        // publishProgress("sleep");
                    } catch (InterruptedException e) {
                        ;
                    }
                } else {
                    counter = 0;
                    MAVLinkMessage msg = StateBuffer.RECEIEVEDATAQUEUE.poll();
                    //publishProgress("poll");
                    if (msg != null || !msg.equals(null)) {
                        switch(msg.messageType){
                            case 30:
                               valuse[0]=Float.toString(((msg_attitude) msg).pitch);
                                valuse[1]=Float.toString(((msg_attitude) msg).roll);
                                valuse[2]=Float.toString(((msg_attitude) msg).yaw);
                                break;
                        }
                        publishProgress(valuse);

                    }
                }
                if (counter >= 1000) break;
            }
            return null;
        }
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
