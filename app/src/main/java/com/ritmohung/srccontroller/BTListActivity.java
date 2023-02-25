package com.ritmohung.srccontroller;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import java.util.Set;

import es.dmoral.toasty.Toasty;

public class BTListActivity extends AppCompatActivity {

    public static String EXTRA_DEVICE_ADDRESS = "device_address";
    public static String EXTRA_DEVICE_NAME = "device_name";

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_btlist);
        setResult(Activity.RESULT_CANCELED);

        // Sets up action bar
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            //actionBar.setHomeButtonEnabled(true);
        }

        // ListView layout & OnItemClickListener
        ArrayAdapter<String> pairedDevicesArrayAdapter =
                new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, 0);
        ListView listPairedDevices = findViewById(R.id.pairedDevicesListView);
        listPairedDevices.setAdapter(pairedDevicesArrayAdapter);
        listPairedDevices.setOnItemClickListener(mDeviceClickListener);

        // Bluetooth Adapter
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();
        // List out all paired devices to ListView
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices)
                pairedDevicesArrayAdapter.add(device.getName() + "\n" + device.getAddress());
        }
        else
            Toasty.warning(BTListActivity.this, R.string.BT_NO_PAIRED, Toast.LENGTH_SHORT, true).show();
    }



    // OnItemClickListener: Handles clicking on devices
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length() - 17);
            String name = info.substring(0, info.length() - 18);

            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);
            intent.putExtra(EXTRA_DEVICE_NAME, name);
            setResult(Activity.RESULT_OK, intent);
            finish();
        }
    };
}