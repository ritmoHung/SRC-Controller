package com.ritmohung.srccontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;

import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import es.dmoral.toasty.Toasty;

public class BTTX {
    public Context context;
    public TextView command;
    private BluetoothAdapter btAdapter = null;
    private BluetoothDevice device = null;
    private BluetoothSocket socket = null;
    private InputStream inStream = null;
    private OutputStream outStream = null;
    private TXThread txThread;
    public boolean CONNECTED = false;
    public String s = "";

    private static final int ENABLE_BT_RCODE = 3;
    private static final UUID MY_UUID_SECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");



    // Constructor
    public BTTX(Context context, TextView textView) {
        this.context = context;
        command = textView;
    }

    // Functions
    public void init() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter == null) {
            Toasty.error(context, R.string.BT_NA, Toast.LENGTH_SHORT, true).show();
        }
        else {
            if(!btAdapter.isEnabled()) {
                Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                ((Activity) context).startActivityForResult(intent, ENABLE_BT_RCODE);
            }
            else
                Toasty.info(context, R.string.BT_ALREADY_ON, Toast.LENGTH_SHORT, true).show();
        }
    }

    public boolean reset() {
        if(inStream != null) {
            try {
                inStream.close();
            } catch(Exception e) {
                Log.e("InStream: ", "Close failed");
                return false;
            }
            inStream = null;
        }
        if(outStream != null) {
            try {
                outStream.close();
            } catch(Exception e) {
                Log.e("OutStream: ", "Close failed");
                return false;
            }
            outStream = null;
        }

        if(socket != null) {
            try {
                socket.close();
            } catch(Exception e) {
                Log.e("Socket: ", "Close failed");
                return false;
            }
            socket = null;
        }

        CONNECTED = false;
        return true;
    }

    public boolean connect(String address) {
        reset();

        // Adapter: Get target address
        if(device == null)
            device = btAdapter.getRemoteDevice(address);

        // Socket: Bind to RFCOMM by UUID
        try {
            socket = device.createInsecureRfcommSocketToServiceRecord(MY_UUID_SECURE);
        } catch(IOException e) {
            Log.e("BT: ", "Socket creation failed", e);
            return false;
        }

        // Socket: Try connection
        btAdapter.cancelDiscovery();
        try {
            socket.connect();
            CONNECTED = true;
            Toasty.success(context, R.string.BT_SUCCESS, Toast.LENGTH_SHORT, true).show();
        } catch(IOException e) {
            try {
                socket.close();
                Log.e("BT: ", "Socket failed but closed successfully", e);
            } catch(IOException e2) {
                Log.e("BT: ", "Socket during connection failure", e2);
            }
            return false;
        }

        // Connected
        // IOStream: Attach to socket
        try {
            inStream = socket.getInputStream();
            outStream = socket.getOutputStream();
        } catch(IOException e) {
            Log.e("BT:", "IOStream failed", e);
            return false;
        }

        txThread = new TXThread();
        txThread.start();
        return true;
    }


    public class TXThread extends Thread {
        public TXThread() {}

        public void run() {
            while(true) {
                if(!s.equals("")) {
                    try {
                        outStream.write(s.getBytes(StandardCharsets.UTF_8));
                        outStream.flush();
                        Log.d("BT: ", "Connected, sending");
                        try {
                            Thread.sleep(10);
                        } catch(Exception e) {
                            Log.e("BT: ", "Connected, error in reading buffer");
                        }
                    } catch(IOException e) {
                        Log.e("BT: ", "Connected, output failed", e);
                        Toasty.warning(context, R.string.BT_IO_FAIL, Toast.LENGTH_SHORT, true).show();
                    }
                    s = "";
                }

                // Interrupt
                if(Thread.interrupted())
                    return;
            }
        }
    }
}