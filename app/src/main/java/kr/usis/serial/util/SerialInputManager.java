/**
 * Created by 최용득(Daniel) on 2015-07-15.
 */


package kr.usis.serial.util;

import android.hardware.usb.UsbRequest;
import android.util.Log;

import org.mavlink.MAVLinkReader;
import org.mavlink.messages.MAVLinkMessage;

import kr.usis.serial.driver.UsbSerialPort;
import kr.usis.u_drone.StateBuffer;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;


public class SerialInputManager implements Runnable {

    private static final String TAG = SerialInputManager.class.getSimpleName();
    private static final boolean DEBUG = true;

    private static final int READ_WAIT_MILLIS = 200;
    private static final int BUFSIZ = 4096;

    private static final int HEARTBEAT = 0;
    private static final int OTHERS = 1;



    private final UsbSerialPort mDriver;

    private final ByteBuffer mReadBuffer = ByteBuffer.allocate(BUFSIZ);

    // Synchronized by 'mWriteBuffer'
    private final ByteBuffer mWriteBuffer = ByteBuffer.allocate(BUFSIZ);

    private enum State {
        STOPPED,
        RUNNING,
        STOPPING
    }

    // Synchronized by 'this'
    private State mState = State.STOPPED;

    // Synchronized by 'this'
    private Listener mListener;


    public interface Listener {
        /**
         * Called when new incoming data is available.
         */
        public void onNewData(String data);
        //public void onNewData(MAVLinkMessage data);
        /**
         * Called when {@link SerialInputManager#run()} aborts due to an
         * error.
         */
        public void onRunError(Exception e);
    }

    /**
     * Creates a new instance with no listener.
     */
    public SerialInputManager(UsbSerialPort driver) {
        this(driver, null);
    }

    /**
     * Creates a new instance with the provided listener.
     */
    public SerialInputManager(UsbSerialPort driver, Listener listener) {
        mDriver = driver;
        mListener = listener;
    }

    public synchronized void setListener(Listener listener) {
        mListener = listener;
    }

    public synchronized Listener getListener() {
        return mListener;
    }

    public void writeAsync(byte[] data) {
        synchronized (mWriteBuffer) {
            mWriteBuffer.put(data);
        }
    }

    public synchronized void stop() {
        if (getState() == State.RUNNING) {
            Log.i(TAG, "Stop requested");
            mState = State.STOPPING;
        }
    }

    private synchronized State getState() {
        return mState;
    }

    /**
     * Continuously services the read and write buffers until {@link #stop()} is
     * called, or until a driver exception is raised.
     *
     * NOTE(mikey): Uses inefficient read/write-with-timeout.
     * TODO(mikey): Read asynchronously with {@link UsbRequest#queue(ByteBuffer, int)}
     */



    @Override
    public void run() {

        synchronized (this) {
            if (getState() != State.STOPPED) {
                throw new IllegalStateException("Already running.");
            }
            mState = State.RUNNING;
        }

        Log.i(TAG, "Running ..");
        try {
            while (true) {
                if (getState() != State.RUNNING) {
                    Log.i(TAG, "Stopping mState=" + getState());
                    break;
                }
                step(); // call step function below
            }
        } catch (Exception e) {
            Log.w(TAG, "Run ending due to exception: " + e.getMessage(), e);
            final Listener listener = getListener();
            if (listener != null) {
              listener.onRunError(e);
            }
        } finally {
            synchronized (this) {
                mState = State.STOPPED;
                Log.i(TAG, "Stopped.");
            }
        }
    }

    // put separated packet into Mavlink
    // 수신된 패킷의 종류에 맞게 Queue에 집어넣음
    private void MavLinkFactory(byte[] data, int type){
        try {
            DataInputStream e = new DataInputStream(new ByteArrayInputStream(data));
            MAVLinkReader reader = new MAVLinkReader(e, (byte) -2);
            MAVLinkMessage msg;
            try {
                msg = reader.getNextMessage();
            } catch (Exception var6) {
                e.close();
                return;
            }

            // push data to queue if its not empty - Daniel
            if (msg != null || !msg.equals(null)) {
                if(type == OTHERS) {
                    StateBuffer.RECEIEVEDATAQUEUE.offer(msg);
                }
                else if(type == HEARTBEAT){
                    StateBuffer.HEARTBEATQUEUE.offer(msg);
                }
            }
            e.close();
        }
        catch(Exception var6){

        }
    }

    /*
    private void MAVLinkHeartBeat(byte[] data){
        try {
            DataInputStream e = new DataInputStream(new ByteArrayInputStream(data));
            MAVLinkReader reader = new MAVLinkReader(e, (byte) -2);
            MAVLinkMessage msg;
            try {
                msg = reader.getNextMessage();
            } catch (Exception var6) {
                e.close();
                return;
            }
            if (msg != null || !msg.equals(null)) {
                StateBuffer.HEARTBEATQUEUE.offer(msg);
            }
            e.close();
        }
        catch(Exception var6){

        }
    }*/

//Daniel
//데이터 수신 부분.
//여러개의 패킷이 연달아서 한번에 수신됨
//따라서 패킷의 처음과 끝을 확인하여 나눠주는 작업을 먼저 함
    private void step() throws IOException {
        byte [] tmp = new byte[64];
        int j = 0;
        // Handle incoming data.
        int len = mDriver.read(mReadBuffer.array(), READ_WAIT_MILLIS);
        if (len > 0) {
            if (DEBUG) Log.d(TAG, "Read data len=" + len);
            final Listener listener = getListener();
            if (listener != null) {
                final byte[] data = new byte[len];
                mReadBuffer.get(data, 0, len);
                try{                                        //Daniel
                    Arrays.fill(tmp, (byte) 0);             //reset array to zero
                    for(int i = 0;i < data.length; i++){   // Separate packets
                        if((data[i]&0xFF)!=254){            //패킷 나눠주기
                            continue;
                        } else {
                            while((i < data.length - 1 && (data[i+1]&0xFF)!=254)){
                                tmp[j++] = data[i++];
                            }
                            tmp[j] = data[i]; j = 0;

                            switch((tmp[5]&0xff)){
                                case 1:                 //SYS_STATUS
                                case 24:                //GPS_RAW
                                case 30:                //ATTITUDE
                                case 33:                //GLOBAL_POSITION_INT
                                case 35:                //RC_CHANNELS_RAW
                                case 74:                //VFR_HUD
                                case 166:               //msg_radio(rssi)
                                case 91:
                                    MavLinkFactory(tmp,OTHERS); //나눠진 패킷 여기다가 넣음
                                    break;
                                case 0:               //heartbeat
                                    MavLinkFactory(tmp,HEARTBEAT); //여기도
                                    break;
                            }
                        }
                    }
                } catch(Exception d){ }
            }
            mReadBuffer.clear();
        }
    }

}
