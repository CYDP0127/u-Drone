package kr.usis.u_drone;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.MAV_AUTOPILOT;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_COMPONENT;
import org.mavlink.messages.MAV_MODE;
import org.mavlink.messages.MAV_MODE_FLAG;
import org.mavlink.messages.MAV_TYPE;
import org.mavlink.messages.ardupilotmega.msg_attitude;
import org.mavlink.messages.ardupilotmega.msg_command_long;
import org.mavlink.messages.ardupilotmega.msg_heartbeat;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_override;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_raw;
import org.w3c.dom.Text;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.usis.serial.act.DeviceListActivity;
import kr.usis.serial.util.HexDump;
import kr.usis.serial.util.SerialInputOutputManager;

public class MainActivity extends FragmentActivity {

    TextView[] textView = new TextView[30];

    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(1024);
    boolean DisconnectedFlag = false;
    DeviceListActivity dla;
    BackThread mThread;
    HBReceive hbrThread;
    HBSend hbsThread;
    //TakeOff toThread;

    Toast toast;


    private SerialInputOutputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    //Handler for getting start Input Thread. - Daniel
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                startIoManager();
            }
            if (msg.what == 1) {
                //heartbeat receiving error
                showErrorMsg("HEARTBEAT RECEIVING ERROR");
            }
            if(msg.what == 255 || msg.what == 200){
                showErrorMsg("HEARTBEAT RECEIVING ERROR !!!!");
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

        if (StateBuffer.CONNECTION != null) {
            //execute heartbeat receive thread when its connected


            mSerialIoManager = new SerialInputOutputManager(StateBuffer.CONNECTION, mListener);
            mExecutor.submit(mSerialIoManager);


            //execute heartbeat receive thread when its connected
            hbrThread = new HBReceive(mHandler);
            hbrThread.setDaemon(true);
            hbrThread.start();

            //execute heartbeat send thread when its connected
            hbsThread = new HBSend();
            hbsThread.start();

        }
    }


    public void TakeOff(View v) throws IOException {
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

        mWriteBuffer.clear();
        mWriteBuffer.put(arm.encode());

        synchronized (mWriteBuffer) {
            int len = mWriteBuffer.position();
            if (len > 0) {
                buff = new byte[41];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buff, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (buff != null) {
            StateBuffer.CONNECTION.write(buff, 1000);
        }

    }


    public void ARM(View v) throws IOException {
        byte[] buff = null;
        msg_command_long arm = new msg_command_long(1, 1);
        arm.param1 = 1;
        arm.param2 = 0;
        arm.param3 = 0;
        arm.param4 = 0;
        arm.param5 = 0;
        arm.param6 = 0;
        arm.param7 = 0;

        arm.sequence = StateBuffer.increaseSequence();

        arm.target_system = 1;
        arm.target_component = 1;

        arm.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
        arm.confirmation = 1;
        mWriteBuffer.clear();

        mWriteBuffer.put(arm.encode());
        synchronized (mWriteBuffer) {
            int len = mWriteBuffer.position();
            if (len > 0) {
                buff = new byte[41];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buff, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (buff != null) {
            StateBuffer.CONNECTION.write(buff, 10000);
        }

    }


    public void DisARM(View v) throws IOException {
        byte[] buff = null;
        msg_command_long arm = new msg_command_long(1, 1);
        arm.param1 = 0;
        arm.param2 = 0;
        arm.param3 = 0;
        arm.param4 = 0;
        arm.param5 = 0;
        arm.param6 = 0;
        arm.param7 = 0;

        arm.sequence = StateBuffer.increaseSequence();

        arm.target_system = 1;
        arm.target_component = 1;
        arm.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
        arm.confirmation = 0;
        mWriteBuffer.clear();
        mWriteBuffer.put(arm.encode());
        synchronized (mWriteBuffer) {
            int len = mWriteBuffer.position();
            if (len > 0) {
                buff = new byte[41];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buff, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (buff != null) {
            StateBuffer.CONNECTION.write(buff, 10000);
        }

    }


    //to show up error message box
    public void showErrorMsg(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("Error Message Box")
                .setMessage(str)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do some thing here which you need
                    }
                });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);

        textView[0] = (TextView) findViewById(R.id.textView15);     //PITCH
        textView[1] = (TextView) findViewById(R.id.textView17);    //ROLL
        textView[2] = (TextView) findViewById(R.id.textView18);     //YAW




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

    public class Dequeue extends AsyncTask<Void, String, Void> {

        @Override
        protected void onProgressUpdate(String... strings) {
            for (int i = 0; i < 3; i++) {
                textView[i].setText(strings[i]);
            }
        }


        @Override
        protected Void doInBackground(Void... arg0) {
            int counter = 0;
            String[] valuse = new String[10];
            while (true) {
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
                    if (msg != null || !msg.equals(null)) {
                        switch (msg.messageType) {
                            case 30:
                                valuse[0] = Float.toString(((msg_attitude) msg).pitch);
                                valuse[1] = Float.toString(((msg_attitude) msg).roll);
                                valuse[2] = Float.toString(((msg_attitude) msg).yaw);
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
