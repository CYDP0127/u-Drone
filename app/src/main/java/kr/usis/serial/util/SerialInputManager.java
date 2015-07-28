/* Copyright 2011-2013 Google Inc.
 * Copyright 2013 mike wakerly <opensource@hoho.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301,
 * USA.
 *
 * Project home page: https://github.com/mik3y/usb-serial-for-android
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

/**
 * Utility class which services a {@link UsbSerialPort} in its {@link #run()}
 * method.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
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

            }
            e.close();
        }
        catch(Exception var6){

        }
    }*/


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
                        if((data[i]&0xFF)!=254){
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
                                    MavLinkFactory(tmp, OTHERS);
                                    break;
                                case 0:               //heartbeat
                                    MavLinkFactory(tmp, HEARTBEAT);
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
