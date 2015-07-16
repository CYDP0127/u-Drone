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

package kr.usis.serial.act;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import kr.usis.serial.driver.UsbSerialPort;
import kr.usis.serial.util.HexDump;
import kr.usis.serial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Monitors a single {@link UsbSerialPort} instance, showing all data
 * received.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class SerialConsoleActivity{

    private final String TAG = SerialConsoleActivity.class.getSimpleName();
    Context mContext;
    /**
     * Driver instance, passed in statically via
     * {@link #show(Context, UsbSerialPort)}.
     *
     * <p/>
     * This is a devious hack; it'd be cleaner to re-create the driver using
     * arguments passed in with the {@link #startActivity(Intent)} intent. We
     * can get away with it because both activities will run in the same
     * process, and this is a simple demo.
     */
    private static UsbSerialPort sPort = null;
    public byte[] recieved = new byte[200];
   // private TextView mTitleTextView;
    private TextView mDumpTextView;
    private ScrollView mScrollView;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private SerialInputOutputManager mSerialIoManager;

    public SerialConsoleActivity(Context mContext, UsbSerialPort port) {
        sPort = port;
        this.mContext = mContext;
        Toast toast = Toast.makeText(mContext, "SerialConsoleActivity", Toast.LENGTH_LONG);
        toast.show();
    }

  /*  private final SerialInputOutputManager.Listener mListener =
            new SerialInputOutputManager.Listener() {

                @Override
                public void onRunError(Exception e) {
                    Log.d(TAG, "Runner stopped.");
                }

                @Override
                public void onNewData(final byte[] data) {
                    SerialConsoleActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            SerialConsoleActivity.this.updateReceivedData(data);
                        }
                    });
                }
            };*/

/*    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }*/

   /* @Override
    protected void onPause() {
        super.onPause();
        stopIoManager();
        if (sPort != null) {
            try {
                sPort.close();
            } catch (IOException e) {
                // Ignore.
            }
            sPort = null;
        }
        finish();
    }*/


    public void getConnect() {
        Toast toast = Toast.makeText(mContext, "getConnect", Toast.LENGTH_SHORT);
        toast.show();
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
          //  mTitleTextView.setText("No serial device.");
            toast = Toast.makeText(mContext, "No serial device.", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            final UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);

            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice());
            if (connection == null) {
              //  mTitleTextView.setText("Opening device failed");
                toast = Toast.makeText(mContext, "Opening device failed", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);
                toast = Toast.makeText(mContext, "connected!!", Toast.LENGTH_LONG);
                toast.show();

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
              //  mTitleTextView.setText("Error opening device: " + e.getMessage());
                toast = Toast.makeText(mContext, "Error opening device", Toast.LENGTH_SHORT); toast.show();
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
           // mTitleTextView.setText("Serial device: " + sPort.getClass().getSimpleName());
            toast = Toast.makeText(mContext, "serial device", Toast.LENGTH_SHORT); toast.show();
        }
       // onDeviceStateChange();
    }

   /* private void stopIoManager() {
        if (mSerialIoManager != null) {
            Log.i(TAG, "Stopping io manager ..");
            mSerialIoManager.stop();
            mSerialIoManager = null;
        }
    }*/

    private void startIoManager() {
        if (sPort != null) {
            Log.i(TAG, "Starting io manager ..");
            new Thread(new SerialInputOutputManager(sPort)).start();
          //  mSerialIoManager = new SerialInputOutputManager(sPort, mListener);
           // mExecutor.submit(mSerialIoManager);
        }
    }

    private void onDeviceStateChange() {
       // stopIoManager();
        startIoManager();
    }

    //recieve
    private void updateReceivedData(byte[] data) {
      //  this.recieved = data;
        /*final String message = "Read " + data.length + " bytes: \n"
                + HexDump.dumpHexString(data) + "\n\n";
        mDumpTextView.append(message);
        mScrollView.smoothScrollTo(0, mDumpTextView.getBottom());*/



    }
    /**
     * Starts the activity, using the supplied driver instance.
     *
     * //@param context
     */ //@param driver

   // public void SerialConsoleActivity(Context context, UsbSerialPort port) {


/*        final Intent intent = new Intent(context, SerialConsoleActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_NO_HISTORY);
        context.startActivity(intent);*/
    //}

}
