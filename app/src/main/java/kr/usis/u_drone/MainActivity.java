package kr.usis.u_drone;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.mavlink.IMAVLinkCRC;
import org.mavlink.MAVLinkCRC;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_COMPONENT;
import org.mavlink.messages.MAV_FRAME;
import org.mavlink.messages.MAV_SET_MODE;
import org.mavlink.messages.ardupilotmega.msg_attitude;
import org.mavlink.messages.ardupilotmega.msg_command_long;
import org.mavlink.messages.ardupilotmega.msg_gps_raw_int;
import org.mavlink.messages.ardupilotmega.msg_heartbeat;
import org.mavlink.messages.ardupilotmega.msg_hil_controls;
import org.mavlink.messages.ardupilotmega.msg_mission_ack;
import org.mavlink.messages.ardupilotmega.msg_mission_count;
import org.mavlink.messages.ardupilotmega.msg_mission_item;
import org.mavlink.messages.ardupilotmega.msg_mission_request;
import org.mavlink.messages.ardupilotmega.msg_radio;
import org.mavlink.messages.ardupilotmega.msg_rc_channels_override;
import org.mavlink.messages.ardupilotmega.msg_set_mode;
import org.mavlink.messages.ardupilotmega.msg_sys_status;
import org.mavlink.messages.ardupilotmega.msg_vfr_hud;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import kr.usis.serial.act.DeviceListActivity;
import kr.usis.serial.util.SerialInputManager;

public class MainActivity extends FragmentActivity {

    TextView[] textView = new TextView[30];
    Button b_throttle_up;

    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);
    boolean DisconnectedFlag = false;
    boolean home_Coordinate = true;
    boolean init_voltage = true;
    boolean state_takeoff = false;
    DeviceListActivity dla;
    BackThread mThread;
    HBReceive hbrThread;
    HBSend hbsThread;
    ThreadChSend chsendThread;
    CntProcedure cntThread;
    EnRThread enrThread;


    long longitude;
    long latitude;
    long altitude;
    long home_logitude = 0;
    long home_latitude = 0;
    long home_altitude = 0;
    float elasped_time;
    float remain_time;
    float voltage = 1;

    final static int NEUTRAL = 1505;
    final static int MAINTAIN = 65535;

    private SerialInputManager mSerialIoManager;
    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    //Handler for getting start Input Thread. - Daniel
    Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                //when it's connected then call below function
                startIoManager();
            }
            if (msg.what == 1) {
                //heartbeat receiving error
                showErrorMsg("HEARTBEAT RECEIVING ERROR");
            }
            if(msg.what == 2){
                elasped_time = msg.arg1;
                remain_time = msg.arg2;
                DisplayEnRTime((elasped_time / 100) % 60, remain_time / 100);
            }

            if (msg.what == 255 || msg.what == 200) {
                showErrorMsg("HEARTBEAT RECEIVING ERROR !!!!");
            }
        }

    };

    //to display elasped time, remain time
    public void DisplayEnRTime(float elasped_time, float remain_time){
        textView[9].setText(String.format("%.2f",elasped_time));
        textView[10].setText(String.format("%.2f",remain_time));
    }

    //handler for displaying recieved data. - Daniel (test module)
    private final SerialInputManager.Listener mListener =
            new SerialInputManager.Listener() {

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

    //display input data(test module)
    private void updateReceivedData(String data) {
/*        textview.append(data);
        textview.append(" ");
        textview.invalidate();*/
    }

    public void sleep(int ms){
        try {
            Thread.sleep(ms);
            // publishProgress("sleep");
        } catch (InterruptedException e) {
            ;
        }
    }

    private void startIoManager() {
        //Daniel
        if (StateBuffer.CONNECTION != null) {

            //execute data receiving thread
            mSerialIoManager = new SerialInputManager(StateBuffer.CONNECTION, mListener);
            mExecutor.submit(mSerialIoManager);

            //execute heartbeat receiving thread
            hbrThread = new HBReceive(mHandler);
            hbrThread.setDaemon(true);
            hbrThread.start();

            //execute heartbeat sending thread
            hbsThread = new HBSend();
            hbsThread.start();

            //execute channel data sending thread
            chsendThread = new ThreadChSend();
            chsendThread.start();

            //execute connecetion Thread.
            //when it's connected this Thread send some packets for requesting data which we need
            cntThread = new CntProcedure();
            cntThread.start();

        }
    }


    // Setting mode
    //??�?? ??? ???? ??? ???
    public void SetMode(int mode) throws IOException {
        byte[] buff = null;
        msg_set_mode message = new msg_set_mode(1, 1);
        message.target_system = (byte)mode;
        message.base_mode = 0;
        message.custom_mode=0x101;
        message.sequence = StateBuffer.increaseSequence();
        mWriteBuffer.put(message.encode());
        synchronized (mWriteBuffer) {
            int len = mWriteBuffer.position();
            if (len > 0) {
                buff = new byte[14];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buff, 0, len);
                mWriteBuffer.clear();
            }
        }
        if (buff != null) {
            StateBuffer.CONNECTION.write(buff, 1000);
        }
    }

    public msg_rc_channels_override getChannelOvr() {
        msg_rc_channels_override msgovr = new msg_rc_channels_override(255, 190);
        msgovr.sequence = StateBuffer.increaseSequence();
        msgovr.target_component = MAV_COMPONENT.MAV_COMP_ID_MISSIONPLANNER;
        msgovr.target_system = 1;
        return msgovr;
    }

    public msg_mission_item getMissionItem(long latitude, long longitude, long altitude, int command) {
        msg_mission_item msg = new msg_mission_item(1, 1);
        msg.x = latitude;
        msg.y = longitude;
        msg.z = altitude;
        msg.seq = 0;
        msg.target_system = 1;
        msg.target_component = 1;
        msg.frame = (byte) MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        //  msg.frame = MAV_FRAME.MAV_FRAME_GLOBAL_RELATIVE_ALT;
        msg.current = 0;
        msg.autocontinue = 1;
        msg.command = command;
        msg.sequence = StateBuffer.increaseSequence();
        return msg;
    }


    //ARM Button event
    //ARM ??? ????
    public void ARM(View v) throws IOException {
        if (StateBuffer.CREATEDCONNECTION) {
            SetMode(MAV_SET_MODE.STABILIZE);
            sleep(100);
            SetMode(MAV_SET_MODE.STABILIZE);
            sleep(100);
            SetMode(MAV_SET_MODE.STABILIZE);
            sleep(1000);
            Arming();
            sleep(100);
            _Get_MissionReq();
            sleep(100);
            TakeOffInit();
        }
    }


    //??????
    //????
    public void Arming() throws IOException {
        byte[] buffer = null;
        msg_command_long msg = new msg_command_long(1, 1);
        msg.param1 = 1;
        msg.param2 = 0;
        msg.param3 = 0;
        msg.param4 = 0;
        msg.param5 = 0;
        msg.param6 = 0;
        msg.param7 = 0;
        msg.sequence = StateBuffer.increaseSequence();
        msg.target_system = 1;
        msg.target_component = 1;
        msg.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
        //MAV_CMD_COMPONENT_CONTROL = 250;
        msg.confirmation = 0;
        mWriteBuffer.put(msg.encode());
        synchronized (mWriteBuffer) {
            int len = mWriteBuffer.position();
            if (len > 0) {
                buffer = new byte[41];
                mWriteBuffer.rewind();
                mWriteBuffer.get(buffer, 0, len);
                mWriteBuffer.clear();
            }
        }

        if (buffer != null) {
            StateBuffer.CONNECTION.write(buffer, 10000);
        }

    }


    //Mission Request
    //????
    public void _Get_MissionReq() throws IOException {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_mission_request message = new msg_mission_request(1, 1);
        message.target_system = 1;
        message.target_component = 1;
        message.seq = 0;
        message.sequence = StateBuffer.increaseSequence();
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }


    //Arming??? Throttle ?? ???? ?�?????? ????? ??????? Arming???�?? ??????
    //????
    public void TakeOffInit() throws IOException {
        msg_rc_channels_override msg = getChannelOvr();
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg.chan1_raw = MAINTAIN;
        msg.chan2_raw = MAINTAIN;
        msg.chan3_raw = 1105;
        msg.chan4_raw = MAINTAIN;
        msg.chan5_raw = MAINTAIN;
        msg.chan6_raw = MAINTAIN;
        msg.chan7_raw = MAINTAIN;
        msg.chan8_raw = MAINTAIN;
        StateBuffer.BufferStorage.offer(msg.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }


    // TAKEOFF Button event
    // Probably have to put sleep between functions.
    // TAKEOFF ??? ????.
    public void TakeOff(View v) throws Exception {
        //Arming();
        if (StateBuffer.CREATEDCONNECTION) {
            call_mission_count();
            GetMsg_Waypoint();
            takeoff_();
            GetMissionItem_LoiterUnli();
            call_mission_accepted();
            SetMode(MAV_SET_MODE.AUTO);
            DuringTakingOff();
            state_takeoff = true;
        }
    }

    //Land Event
    public void Landing(View v) throws IOException{
        if(state_takeoff){
            SetMode(MAV_SET_MODE.LAND);
            sleep(100);
            state_takeoff = false;
        }
    }

    //??? ????
    //????
    public void call_mission_count() throws Exception {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_mission_count message = new msg_mission_count(1, 1);
        message.sequence = StateBuffer.increaseSequence();
        message.target_component = 1;
        message.target_system = 1;
        message.count = 0;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    //Home??? ???????
    //????
    public void GetMsg_Waypoint() throws IOException {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_mission_item message = getMissionItem(latitude, longitude, altitude, MAV_CMD.MAV_CMD_NAV_WAYPOINT);
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    //????????? ???
    public void takeoff_() throws Exception {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_mission_item message = getMissionItem(0, 0, altitude, MAV_CMD.MAV_CMD_NAV_TAKEOFF);
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    public void GetMissionItem_LoiterUnli() throws Exception {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_mission_item message = getMissionItem(home_latitude, home_logitude, home_altitude, MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM);
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    public void call_mission_accepted() throws Exception {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_mission_ack message = new msg_mission_ack(1, 1);
        message.sequence = StateBuffer.increaseSequence();
        message.target_component = 1;
        message.target_system = 1;
        message.type = 0;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    //TAKEOFF?? Throttle, pitch, roll ???? ??????? ??��?.
    public void DuringTakingOff() throws Exception {
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_rc_channels_override msg = getChannelOvr();
        msg.chan1_raw = 1505;
        msg.chan2_raw = 1505;
        msg.chan3_raw = 1505;
        msg.chan4_raw = 65535;
        msg.chan5_raw = 65535;
        msg.chan6_raw = 65535;
        msg.chan7_raw = 65535;
        msg.chan8_raw = 65535;
        StateBuffer.BufferStorage.offer(msg.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    /*
    //Send Throttle event;
    public void Throttle_send(View v) throws IOException{
        int id = v.getId();
        int throttle = 1105;

        if(id == R.id.throttle_up)
            throttle = 1160;

        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_rc_channels_override msg = getChannelOvr();
        msg.chan1_raw = 65535;
        msg.chan2_raw = 65535;
        msg.chan3_raw = throttle;
        msg.chan4_raw = 65535;
        msg.chan5_raw = 65535;
        msg.chan6_raw = 65535;
        msg.chan7_raw = 65535;
        msg.chan8_raw = 65535;
=======
        msg.chan1_raw = NEUTRAL;
        msg.chan2_raw = NEUTRAL;
        msg.chan3_raw = NEUTRAL;
        msg.chan4_raw = MAINTAIN;
        msg.chan5_raw = MAINTAIN;
        msg.chan6_raw = MAINTAIN;
        msg.chan7_raw = MAINTAIN;
        msg.chan8_raw = MAINTAIN;
>>>>>>> Stashed changes
        StateBuffer.BufferStorage.offer(msg.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }
    */

    public void Manual(View v) throws IOException{
        if(StateBuffer.CREATEDCONNECTION) {
            StateBuffer.flagThread_ch_send_Run = false;
            sleep(100);
            msg_rc_channels_override msg = getChannelOvr();
            msg.chan1_raw = 0;
            msg.chan2_raw = 0;
            msg.chan3_raw = 0;
            msg.chan4_raw = 0;
            msg.chan5_raw = 0;
            msg.chan6_raw = 0;
            msg.chan7_raw = 0;
            msg.chan8_raw = 0;
            StateBuffer.BufferStorage.offer(msg.encode());
            StateBuffer.flagThread_ch_send_Run = true;
            sleep(3000);
            StateBuffer.flagThread_ch_send_Run = false;
            chsendThread.stop();
            SetMode(MAV_SET_MODE.ALTHOLD);
        }
    }

    //Send pitch,yaw,roll,throttle ..
    public void Send_Control_Command(int roll, int pitch, int throttle, int yaw) throws IOException{
        SetMode(MAV_SET_MODE.LOITER);
        SetMode(MAV_SET_MODE.LOITER);
        SetMode(MAV_SET_MODE.LOITER);
        StateBuffer.flagThread_ch_send_Run = false;
        sleep(100);
        msg_rc_channels_override msg = getChannelOvr();
        msg.chan1_raw = roll;
        msg.chan2_raw = pitch;
        msg.chan3_raw = throttle;
        msg.chan4_raw = yaw;
        msg.chan5_raw = MAINTAIN;
        msg.chan6_raw = MAINTAIN;
        msg.chan7_raw = MAINTAIN;
        msg.chan8_raw = MAINTAIN;
        StateBuffer.BufferStorage.offer(msg.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }


    //???? ???????? ???
    //to show up error message box
    public void showErrorMsg(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("Error Message Box")
                .setMessage(str)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK..", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //do some thing here which you need
                    }
                });
        //No Event here
        /*builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });*/
        AlertDialog alert = builder.create();
        alert.show();
    }



    //DisArming
    //???????
    public void DisARM(View v) throws IOException {
        if (StateBuffer.CREATEDCONNECTION) {
            byte[] buff = null;
            msg_command_long msg = new msg_command_long(1, 1);
            msg.param1 = 0;
            msg.param2 = 0;
            msg.param3 = 0;
            msg.param4 = 0;
            msg.param5 = 0;
            msg.param6 = 0;
            msg.param7 = 0;
            msg.sequence = StateBuffer.increaseSequence();
            msg.target_system = 1;
            msg.target_component = 1;
            msg.command = MAV_CMD.MAV_CMD_COMPONENT_ARM_DISARM;
            msg.confirmation = 0;
            mWriteBuffer.put(msg.encode());
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
    }

    //Google Map location button event
    public void getLocation(View v) throws IOException {
        Intent myIntent = new Intent(MainActivity.this, mylocation.class);
        //myIntent.putExtra("keyX", "33.867"); //Optional parameters - x
        //myIntent.putExtra("keyY", "151.206"); // y
        //Toast.makeText(this, "Location button pressed", Toast.LENGTH_SHORT).show();
        startActivity(myIntent);
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);


        textView[0] = (TextView) findViewById(R.id.textView15);     //PITCH
        textView[1] = (TextView) findViewById(R.id.textView17);     //ROLL
        textView[2] = (TextView) findViewById(R.id.textView18);     //YAW
        textView[3] = (TextView) findViewById(R.id.textView11);     //rssi
        textView[4] = (TextView) findViewById(R.id.textView12);     //ramrssi
        textView[5] = (TextView) findViewById(R.id.textView23);     //voltage battery
        textView[6] = (TextView) findViewById(R.id.textView24);     //Altitude
        textView[7] = (TextView) findViewById(R.id.textView13);     //HDOP
        textView[8] = (TextView) findViewById(R.id.textView14);     //Number of satellites visible
        textView[9] = (TextView) findViewById(R.id.textView21);     //Elasped Time
        textView[10] = (TextView) findViewById(R.id.textView22);     //Remain Time

        findViewById(R.id.yaw_up).setOnTouchListener(mTouchListener);    //ch1
        findViewById(R.id.yaw_down).setOnTouchListener(mTouchListener);
        findViewById(R.id.pitch_up).setOnTouchListener(mTouchListener);     //ch2
        findViewById(R.id.pitch_down).setOnTouchListener(mTouchListener);
        findViewById(R.id.throttle_up).setOnTouchListener(mTouchListener);    //ch3
        findViewById(R.id.throttle_down).setOnTouchListener(mTouchListener);
        findViewById(R.id.roll_up).setOnTouchListener(mTouchListener);       //ch4
        findViewById(R.id.roll_down).setOnTouchListener(mTouchListener);

    }

    //Button event roll, pitch, throttle, yaw
    Button.OnTouchListener mTouchListener = new View.OnTouchListener() {
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction();
            int id = v.getId();
            int roll = NEUTRAL;
            int pitch = NEUTRAL;
            int throttle = NEUTRAL;
            int yaw = NEUTRAL;
            if(StateBuffer.CREATEDCONNECTION) {
                if (action == MotionEvent.ACTION_DOWN) {
                    switch (id) {
                        case R.id.roll_up:
                            roll = 1600;
                            break; //right
                        case R.id.roll_down:
                            roll = 1400;
                            break; //left
                        case R.id.pitch_up:
                            pitch = 1600;
                            break; //front
                        case R.id.pitch_down:
                            pitch = 1400;
                            break; //back
                        case R.id.throttle_up:
                            throttle = 1760;
                            break; //up
                        case R.id.throttle_down:
                            throttle = 1240;
                            break; //down
                        case R.id.yaw_up:
                            yaw = 1580;
                            break; //right
                        case R.id.yaw_down:
                            yaw = 1420;
                            break; //left
                    }
                    try {
                        Send_Control_Command(roll, pitch, throttle, yaw);
                    } catch (IOException e) {
                    }
                }
                if (action == MotionEvent.ACTION_UP) {
                    switch (id) {
                        default:
                            try {
                                Send_Control_Command(NEUTRAL, NEUTRAL, NEUTRAL, NEUTRAL);
                            } catch (IOException e) {
                            }
                    }
                }
            }
            return false;
        }
    };





    //????
    public void connect(View v) {
        if (!DisconnectedFlag) {
            Toast.makeText(this, "Connection Established", Toast.LENGTH_SHORT).show();
            dla = new DeviceListActivity(this);

            dla.getUSBService(); //Get Telemetry device's Information ?????? ???? ???????

            dla.refreshDeviceList(); //Scan device and connect //??????? ??? ???? ????

            //call connection checking thread
            //???? ??? ??????
            mThread = new BackThread(mHandler);
            mThread.setDaemon(true);
            mThread.start();

            //call Dequeue and Display Thread
            new Dequeue().execute();

            DisconnectedFlag = true;
        }
    }

    //disconnect button event
    public void disconnect(View v) {
        Toast.makeText(this, "Connection Destroyed", Toast.LENGTH_SHORT).show();
        dla = null;
        DisconnectedFlag = false;
        mThread = null;
    }

    //????? ?????? ??? ?????? - ????
    //Thread for checking received data and display
    public class Dequeue extends AsyncTask<Void, String, Void> {

        @Override
        protected void onProgressUpdate(String... strings) {
            for (int i = 0; i < 9; i++) {
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
                    //publishProgress("poll");
                    if (msg != null || !msg.equals(null)) {
                        switch (msg.messageType) {
                            case 30:
                                valuse[0] = Float.toString(((msg_attitude) msg).pitch);
                                valuse[1] = Float.toString(((msg_attitude) msg).roll);
                                valuse[2] = Float.toString(((msg_attitude) msg).yaw);
                                break;
                            case 166:
                                valuse[3] = Integer.toString(((msg_radio) msg).rssi);
                                valuse[4] = Integer.toString(((msg_radio) msg).remrssi);
                                break;
                            case 1:
                                //set current volage and execute displaying Elasped time, Remain Time thread
                                float tmp1 = (((msg_sys_status) msg).voltage_battery) / 1000f;
                                String tmp2 = Float.toString(Math.round(tmp1 * 100f) / 100f);
                                if(init_voltage){
                                    enrThread = new EnRThread(mHandler,Double.parseDouble(tmp2));
                                    enrThread.setDaemon(true);
                                    enrThread.start();
                                    init_voltage = false;
                                }
                                valuse[5] = tmp2 + "V";
                                break;
                            case 74:
                                valuse[6] = Float.toString(Math.round((((msg_vfr_hud) msg).alt) * 100f) / 100f);
                                break;
                            case 24:
                                valuse[7] = Integer.toString(((msg_gps_raw_int) msg).eph);
                                valuse[8] = Integer.toString(((msg_gps_raw_int) msg).satellites_visible);

                                //? ??? ????
                                if (home_Coordinate){
                                    home_logitude = ((msg_gps_raw_int) msg).lon;
                                    home_latitude = ((msg_gps_raw_int) msg).lat;
                                    home_altitude = ((msg_gps_raw_int) msg).alt;
                                    home_Coordinate = false;
                                }
                                longitude = ((msg_gps_raw_int) msg).lon;
                                latitude = ((msg_gps_raw_int) msg).lat;
                                altitude = ((msg_gps_raw_int) msg).alt;
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


