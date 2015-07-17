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
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;
import java.lang.String;
import kr.usis.serial.driver.UsbSerialDriver;
import kr.usis.serial.driver.UsbSerialPort;
import kr.usis.serial.driver.UsbSerialProber;
import kr.usis.serial.util.HexDump;


import java.util.ArrayList;
import java.util.List;

/**
 * Shows a {@link ListView} of available USB devices.
 *
 * @author mike wakerly (opensource@hoho.com)
 */
public class DeviceListActivity {
    String title = new String();
    Context mContext;
    private final String TAG = DeviceListActivity.class.getSimpleName();

    private UsbManager mUsbManager;

    private List<UsbSerialPort> mEntries = new ArrayList<UsbSerialPort>();
    public SerialConsoleActivity sca;

    public DeviceListActivity(Context mContext) {
        this.mContext = mContext;
    }


    public void getUSBService() {
        mUsbManager = (UsbManager) mContext.getSystemService(mContext.USB_SERVICE);
    }

    public void toStartActivity() {
        if(!mEntries.isEmpty()) {
            showConsoleActivity(mEntries.get(0)); //put device to ShowConsoleActivity.
        }
    }

    //Scan and refresh devices - Daniel
    public void refreshDeviceList() {

        new AsyncTask<Void, Void, List<UsbSerialPort>>() {
            @Override
            protected List<UsbSerialPort> doInBackground(Void... params) {
                Log.d(TAG, "Refreshing device list ...");
                SystemClock.sleep(1000);
                final List<UsbSerialDriver> drivers = UsbSerialProber.getDefaultProber().findAllDrivers(mUsbManager);
                final List<UsbSerialPort> result = new ArrayList<UsbSerialPort>();
                for (final UsbSerialDriver driver : drivers) {
                    final List<UsbSerialPort> ports = driver.getPorts();
                    Log.d(TAG, String.format("+ %s: %s port%s",
                            driver, Integer.valueOf(ports.size()), ports.size() == 1 ? "" : "s"));
                    result.addAll(ports);
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<UsbSerialPort> result) {
                mEntries.clear();
                mEntries.addAll(result); //Pass SerialPort values which were scanned - Daniel
                Log.d(TAG, "Done refreshing, " + mEntries.size() + " entries found.");


                if (!mEntries.isEmpty()) {
                    final UsbSerialPort port = mEntries.get(0);
                    final UsbSerialDriver driver = port.getDriver();
                    final UsbDevice device = driver.getDevice();

                    //to display data of node which is scanned - Daniel
                    title = String.format("Vendor %s Product %s",
                            HexDump.toHexString((short) device.getVendorId()),
                            HexDump.toHexString((short) device.getProductId()));
                    Toast toast = Toast.makeText(mContext, title, Toast.LENGTH_LONG);
                    toast.show();

                    toStartActivity();
                    this.cancel(true);
                }
            }
        }.execute((Void) null);

    }

    public void showConsoleActivity(UsbSerialPort port) {
      new SerialConsoleActivity(mContext, port).getConnect();
    }
}



