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

import android.content.Context;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.util.Log;
import android.widget.Toast;

import kr.usis.serial.driver.UsbSerialPort;
import kr.usis.u_drone.StateBuffer;

import java.io.IOException;

public class SerialConsoleActivity{
    Toast toast;
    private final String TAG = SerialConsoleActivity.class.getSimpleName();
    Context mContext;

    private static UsbSerialPort sPort = null;

    //Constructor
    public SerialConsoleActivity(Context mContext, UsbSerialPort port) {
        sPort = port;  this.mContext = mContext;
    }

    //to start connecting
    public void getConnect() {
        Log.d(TAG, "Resumed, port=" + sPort);
        if (sPort == null) {
            toast = Toast.makeText(mContext, "No serial device.", Toast.LENGTH_SHORT);
            toast.show();
        } else {
            final UsbManager usbManager = (UsbManager) mContext.getSystemService(Context.USB_SERVICE);
            UsbDeviceConnection connection = usbManager.openDevice(sPort.getDriver().getDevice()); //Connection here - Daniel
            if (connection == null) {
                toast = Toast.makeText(mContext, "Opening device failed", Toast.LENGTH_SHORT);
                toast.show();
                return;
            }

            try {
                sPort.open(connection);
                sPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE); // Set Serial Communication values - Daniel
                toast = Toast.makeText(mContext, "connected!!", Toast.LENGTH_LONG);
                toast.show();

            } catch (IOException e) {
                Log.e(TAG, "Error setting up device: " + e.getMessage(), e);
                toast = Toast.makeText(mContext, "Error opening device", Toast.LENGTH_SHORT); toast.show();
                try {
                    sPort.close();
                } catch (IOException e2) {
                    // Ignore.
                }
                sPort = null;
                return;
            }
            StateBuffer.CONNECTION = sPort;
            StateBuffer.CREATEDCONNECTION = true;
        }
    }

}
