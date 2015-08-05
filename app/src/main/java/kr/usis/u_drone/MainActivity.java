package kr.usis.u_drone;

/**
 * Created by 최용득(Daniel) on 2015-07-14.
 */

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.mavlink.IMAVLinkCRC;
import org.mavlink.MAVLinkCRC;
import org.mavlink.messages.MAVLinkMessage;
import org.mavlink.messages.MAV_CMD;
import org.mavlink.messages.MAV_FRAME;
import org.mavlink.messages.MAV_SET_MODE;
import org.mavlink.messages.ardupilotmega.msg_attitude;
import org.mavlink.messages.ardupilotmega.msg_command_long;
import org.mavlink.messages.ardupilotmega.msg_gps_raw_int;
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

    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(4096);
    boolean DisconnectedFlag = false;
    DeviceListActivity dla;

    BackThread mThread;
    HBReceive hbrThread;
    HBSend hbsThread;
    ThreadChSend chsendThread;
    CntProcedure cntThread;

    long longitude;
    long latitude;
    long altitude;


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
            if (msg.what == 255 || msg.what == 200) {
                showErrorMsg("HEARTBEAT RECEIVING ERROR !!!!");
            }
        }

    };

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

    public msg_rc_channels_override getChannelOvr() {
        msg_rc_channels_override msgovr = new msg_rc_channels_override(1, 1);
        msgovr.sequence = StateBuffer.increaseSequence();
        msgovr.target_component = 1;
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

    private void startIoManager() {

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

            cntThread = new CntProcedure();
            cntThread.start();
        }
    }

    public void call_mission_accepted() throws Exception {
        msg_mission_ack message = new msg_mission_ack(1, 1);
        message.sequence = StateBuffer.increaseSequence();
        message.target_component = 1;
        message.target_system = 1;
        message.type = 0;
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    public void call_mission_count() throws Exception {
        msg_mission_count message = new msg_mission_count(1, 1);
        message.sequence = StateBuffer.increaseSequence();
        message.target_component = 1;
        message.target_system = 1;
        message.count = 0;
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    public void GetMsg_Waypoint() throws IOException {
        msg_mission_item message = getMissionItem(latitude, longitude, altitude, MAV_CMD.MAV_CMD_NAV_WAYPOINT);
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    public void GetMissionItem_LoiterUnli() throws Exception {
        msg_mission_item message = getMissionItem(latitude, longitude, altitude, MAV_CMD.MAV_CMD_NAV_LOITER_UNLIM);
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    public void takeoff_() throws Exception {
        msg_mission_item message = getMissionItem(0, 0, altitude, MAV_CMD.MAV_CMD_NAV_TAKEOFF);
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }


    public void TakeOffInit() throws IOException {
            msg_rc_channels_override msg = getChannelOvr();
            msg.chan1_raw = 65535;
            msg.chan2_raw = 65535;
            msg.chan3_raw = 1105;
            msg.chan4_raw = 65535;
            msg.chan5_raw = 65535;
            msg.chan6_raw = 65535;
            msg.chan7_raw = 65535;
            msg.chan8_raw = 65535;
            msg.target_component = 1;
            msg.target_system = 1;
            StateBuffer.flagThread_ch_send_Run = false;
            StateBuffer.BufferStorage.offer(msg.encode());
            StateBuffer.flagThread_ch_send_Run = true;
    }

    public void Arming() throws  IOException{
        byte[] buff = new byte[41];

        buff[0] = (byte)254;
        buff[1] = 33;
        buff[2] = 0;
        buff[3] = (byte)255;
        buff[4] = (byte)190;
        buff[5] = 76;
        buff[6] = 0x00; //target system
        buff[7] = 0x00; //target component


        buff[8] = (byte)0x80; //command
        buff[9] = 0x3F;

        buff[10] = 0x00; //confirmation

        //param1
        buff[11] = 0x00;
        buff[12] = 0x00;
        buff[13] = 0x00;
        buff[14] = 0x00;

        //2
        buff[15] = 0x00;
        buff[16] = 0x00;
        buff[17] = 0x00;
        buff[18] = 0x00;

        //3
        buff[19] = 0x00;
        buff[20] = 0x00;
        buff[21] = 0x00;
        buff[22] = 0x00;

        //4
        buff[23] = 0x00;
        buff[24] = 0x00;
        buff[25] = 0x00;
        buff[26] = 0x00;

        //5
        buff[27] = 0x00;
        buff[28] = 0x00;
        buff[29] = 0x00;
        buff[30] = 0x00;

        //6
        buff[31] = 0x00;
        buff[32] = 0x00;
        buff[33] = 0x00;
        buff[34] = (byte)0x90;

        //7
        buff[35] = 0x01;
        buff[36] = 0x01;
        buff[37] = (byte)0xFA;
        buff[38] = 0x00;
        buff[39] = (byte)171;
        buff[40] = (byte)175;
        StateBuffer.CONNECTION.write(buff, 10000);
    }



 /*   public void Arming() throws IOException {
        byte[] buffer = null;
        msg_command_long msg = new msg_command_long(1, 1);
        msg.param1 = 0;
        msg.param2 = 0;
        msg.param3 = 0;
        msg.param4 = 0;
        msg.param5 = 0;
        msg.param6 = 0;
        msg.param7 = 0;
        msg.sequence = StateBuffer.increaseSequence();
        msg.target_system = 0;
        msg.target_component = 0;
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

    }*/

    //ARM Button event
    //ARM 버튼 이벤트
    public void ARM(View v) throws IOException {
        if (StateBuffer.CREATEDCONNECTION) {
            SetMode(MAV_SET_MODE.STABILIZE);
            try {
                Thread.sleep(100);
                // publishProgress("sleep");
            } catch (InterruptedException e) {
                ;
            }
            SetMode(MAV_SET_MODE.STABILIZE);
            try {
                Thread.sleep(100);
                // publishProgress("sleep");
            } catch (InterruptedException e) {
                ;
            }
            SetMode(MAV_SET_MODE.STABILIZE);
            try {
                Thread.sleep(100);
                // publishProgress("sleep");
            } catch (InterruptedException e) {
                ;
            }
            try {
                Thread.sleep(1000);
                // publishProgress("sleep");
            } catch (InterruptedException e) {
                ;
            }

            Arming(); try {
            Thread.sleep(100);
            // publishProgress("sleep");
        } catch (InterruptedException e) {
            ;
        }
            _Get_MissionReq();
        try {
            Thread.sleep(100);
            // publishProgress("sleep");
        } catch (InterruptedException e) {
            ;
        }
          /* TakeOffInit();
        try {
            Thread.sleep(100);
            // publishProgress("sleep");
        } catch (InterruptedException e) {
            ;
        }*/
        }
    }

    // TAKEOFF Button event
    // Probably have to put sleep between functions.
    // TAKEOFF 버튼 이벤트.
    public void TakeOff(View v) throws Exception {
        //Arming();
       /* if (StateBuffer.CREATEDCONNECTION) {
            call_mission_count();
            GetMsg_Waypoint();
            takeoff_();
            GetMissionItem_LoiterUnli();
            call_mission_accepted();
            SetMode(MAV_SET_MODE.AUTO);
            DuringTakingOff();
        }*/
    }

    //TAKEOFF시 Throttle, pitch, roll 값을 중립으로 둔다.
    public void DuringTakingOff() throws Exception {
        msg_rc_channels_override msg = getChannelOvr();
        msg.chan1_raw = 1505;
        msg.chan2_raw = 1505;
        msg.chan3_raw = 1505;
        msg.chan4_raw = 65535;
        msg.chan5_raw = 65535;
        msg.chan6_raw = 65535;
        msg.chan7_raw = 65535;
        msg.chan8_raw = 65535;
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(msg.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }


    // Setting mode
    //드론에 모드 설정 명령 전송
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

    //Mission Request
    public void _Get_MissionReq() throws IOException {
        msg_mission_request message = new msg_mission_request(255, 190);
        message.target_system = 1;
        message.target_component = 1;
        message.seq = 0;
        message.sequence = StateBuffer.increaseSequence();

        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(message.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    //DisArming
    //시동끄기
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


    public void YawToLeft(View v) throws IOException {
        // sending yaw
        byte[] buff = null;
        msg_rc_channels_override msg = getChannelOvr();
        msg.chan1_raw = 65535;  //roll
        msg.chan2_raw = 65535;  //pitch
        msg.chan3_raw = 65535;  //throttle
        msg.chan4_raw = 1400;   //yaw
        msg.chan5_raw = 65535;
        msg.chan6_raw = 65535;
        msg.chan7_raw = 65535;
        msg.chan8_raw = 65535;
        Toast.makeText(this, "Yaw button is pressed", Toast.LENGTH_SHORT).show();
        StateBuffer.flagThread_ch_send_Run = false;
        StateBuffer.BufferStorage.offer(msg.encode());
        StateBuffer.flagThread_ch_send_Run = true;
    }

    //에러 메시지박스 출력
    //to show up error message box
    public void showErrorMsg(String str) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder
                .setTitle("Error Message Box")
                .setMessage(str)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
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
        textView[9] = (TextView) findViewById(R.id.textView25);     //mode



        // Button listeners below
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

    //연결
    public void connect() {
        if (!DisconnectedFlag) {
            Toast.makeText(this, "Connection Established", Toast.LENGTH_SHORT).show();
            dla = new DeviceListActivity(this);

            dla.getUSBService(); //Get Telemetry device's Information 디바이스 정보 갖고오기

            dla.refreshDeviceList(); //Scan device and connect //스켄하고 장치 발견시 연결

            //call connection checking thread
            //연결 확인 쓰레드
            mThread = new BackThread(mHandler);
            mThread.setDaemon(true);
            mThread.start();

            //call Dequeue and Display Thread
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

    //수신된 데이터 출력 쓰레드
    //Thread for checking received data and display
    public class Dequeue extends AsyncTask<Void, String, Void> {

        @Override
        protected void onProgressUpdate(String... strings) {
            for (int i = 0; i < 10; i++) {
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
                                float tmp1 = (((msg_sys_status) msg).voltage_battery) / 1000f;
                                String tmp2 = Float.toString(Math.round(tmp1 * 100f) / 100f);
                                valuse[5] = tmp2 + "V";
                                break;
                            case 74:
                                valuse[6] = Float.toString(Math.round((((msg_vfr_hud) msg).alt) * 100f) / 100f);
                                break;
                            case 24:
                                valuse[7] = Integer.toString(((msg_gps_raw_int) msg).eph);
                                valuse[8] = Integer.toString(((msg_gps_raw_int) msg).satellites_visible);
                                longitude = ((msg_gps_raw_int) msg).lon;
                                latitude = ((msg_gps_raw_int) msg).lat;
                                altitude = ((msg_gps_raw_int) msg).alt;
                                break;
                            case 91:
                               // valuse[9] = Integer.toString(((msg_hil_controls)msg).mode);
                                valuse[9] = "test";
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
