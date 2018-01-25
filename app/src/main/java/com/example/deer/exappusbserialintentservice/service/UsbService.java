package com.example.deer.exappusbserialintentservice.service;

import android.app.IntentService;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.hoho.android.usbserial.driver.UsbSerialDriver;
import com.hoho.android.usbserial.driver.UsbSerialPort;
import com.hoho.android.usbserial.driver.UsbSerialProber;
import com.hoho.android.usbserial.util.HexDump;
import com.hoho.android.usbserial.util.SerialInputOutputManager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by Deer on 6/13/2017 AD.
 */

public class UsbService extends IntentService {

    public static final String ACTION_USB_ATTACHED = "android.hardware.usb.action.USB_DEVICE_ATTACHED";
    public static final String ACTION_USB_DETACHED = "android.hardware.usb.action.USB_DEVICE_DETACHED";

    private UsbManager usbManager;
    private UsbDevice device;
    private UsbDeviceConnection connection;

    private UsbSerialDriver serialDriver;
    private UsbSerialPort serialPort;

    private boolean serialPortConnected = false;

    private SerialInputOutputManager mSerialIoManager;

    private final ExecutorService mExecutor = Executors.newSingleThreadExecutor();

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";

    public UsbService () {
        super("");
    }
    /**
     * Creates an IntentService.  Invoked by your subclass's constructor.
     *
     * @param name Used to name the worker thread, important only for debugging.
     */
    public UsbService(String name) {
        super(name);
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        // Run in Background Thread

//        Intent broadcastIntent = new Intent("UsbServiceIntentServiceUpdate");
//        broadcastIntent.putExtra("counter", "deer");
//        LocalBroadcastManager.getInstance(UsbService.this)
//                .sendBroadcast(broadcastIntent);

        // Get Usb Service
        usbManager = (UsbManager) getSystemService(UsbService.this.USB_SERVICE);

        // Find all available drivers from attached devices
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber()
                .findAllDrivers(usbManager);
        if (!availableDrivers.isEmpty()) {
            // Get Driver
            serialDriver = availableDrivers.get(0);
            // Get Device
            device = serialDriver.getDevice();
            if (device != null) {
                PendingIntent mPendingIntent = PendingIntent
                        .getBroadcast(UsbService.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, mPendingIntent);
            } else {
                connection = null;
                device = null;
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            // TODO: Send an intent to MainActivity
        }

        connection = usbManager.openDevice(device);
        //new ConnectionThread().start();

        serialPort = serialDriver.getPorts().get(0);

        try {
            serialPort.open(connection);
            serialPortConnected = true;
            serialPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

            mSerialIoManager = new SerialInputOutputManager(serialPort, mListener);
            mExecutor.submit(mSerialIoManager);
        } catch (IOException e) {
            e.printStackTrace();
        }

        Toast.makeText(UsbService.this, "connection start", Toast.LENGTH_LONG)
                .show();

        // Find all available drivers from attached devices
//        findSerialPortDevice();

//        IntentFilter filter = new IntentFilter();
//        filter.addAction(ACTION_USB_PERMISSION);
//        registerReceiver(usbReceiver, filter);
//        Toast.makeText(UsbService.this, "IntentFilter", Toast.LENGTH_LONG)
//                .show();
    }

    // Find all available drivers from attached devices
    private void findSerialPortDevice() {
        // This snippet will try to open the first encountered usb device connected, excluding usb root hubs
        List<UsbSerialDriver> availableDrivers = UsbSerialProber.getDefaultProber()
                .findAllDrivers(usbManager);
        if (!availableDrivers.isEmpty()) {
            // Get Driver
            serialDriver = availableDrivers.get(0);
            // Get Device
            device = serialDriver.getDevice();
            if (device != null) {
//                requestUserPermission();
                PendingIntent mPendingIntent = PendingIntent
                        .getBroadcast(UsbService.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                usbManager.requestPermission(device, mPendingIntent);
            } else {
                connection = null;
                device = null;
            }
        } else {
            // There is no USB devices connected. Send an intent to MainActivity
            // TODO: Send an intent to MainActivity
        }
    }

    // Request user permission. The response will be received in the BroadcastReceiver
    private void requestUserPermission() {
        PendingIntent mPendingIntent = PendingIntent
                .getBroadcast(UsbService.this, 0, new Intent(ACTION_USB_PERMISSION), 0);
        usbManager.requestPermission(device, mPendingIntent);
//        Toast.makeText(UsbService.this, "requestUserPermission", Toast.LENGTH_LONG)
//                .show();
    }

    // Different notification from OS will be received here (USB attached, detached, permission responses...)
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) // User accepted our USB connection. Try to open the device as a serial port
                {
                    // TODO: Send an intent to MainActivity
                    Toast.makeText(UsbService.this, "ACTION_USB_PERMISSION", Toast.LENGTH_LONG)
                            .show();
//                    connection = usbManager.openDevice(device);
//                    new ConnectionThread().start();
                } else // User not accepted our USB connection. Send an Intent to the Main Activity
                {
                    // TODO: Send an intent to MainActivity
                }
            } else if (intent.getAction().equals(ACTION_USB_ATTACHED)) {
                if (!serialPortConnected) {
                    findSerialPortDevice(); // A USB device has been attached. Try to open it as a serial port
                }
            } else if (intent.getAction().equals(ACTION_USB_DETACHED)) {
                // USB device was disconnected. Send an Intent to the Main Activity
                // TODO: Send an intent to MainActivity
                if (serialPortConnected) {
                    try {
                        serialPort.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                serialPortConnected = false;
            }
        }
    };

    /*
     * A simple thread to open a serial port.
     * Although it should be a fast operation. moving usb operations away from UI thread is a good thing.
     */
    private class ConnectionThread extends Thread {
        @Override
        public void run() {
            super.run();
            serialPort = serialDriver.getPorts().get(0);
            if (serialPort != null) {
                try {
                    serialPort.open(connection);
                    serialPortConnected = true;
                    serialPort.setParameters(57600, 8, UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE);

                    mSerialIoManager = new SerialInputOutputManager(serialPort, mListener);
                    mExecutor.submit(mSerialIoManager);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                // No driver for given device, even generic CDC driver could not be loaded
                // TODO: Send an intent to MainActivity
            }
        }
    }

    // Data received from serial port will be received here.
    private final SerialInputOutputManager.Listener mListener = new SerialInputOutputManager.Listener() {
        @Override
        public void onNewData(byte[] data) {
            String message = HexDump.dumpHexString(data);
            Log.d("onNewData", message);
            Intent broadcastIntent = new Intent("UsbServiceIntentServiceUpdate");
            broadcastIntent.putExtra("onNewData", message);
            LocalBroadcastManager.getInstance(UsbService.this)
                    .sendBroadcast(broadcastIntent);
        }

        @Override
        public void onRunError(Exception e) {
            Log.d("onRunError", "Runner stopped.");
        }
    };

    // This function will be called from MainActivity to write data through Serial Port
    public void write(byte[] data, int timeoutMillis) throws IOException {
        if (serialPort != null)
            serialPort.write(data, timeoutMillis);
    }
}


















