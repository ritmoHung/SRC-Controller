package com.ritmohung.srccontroller;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.View;

import androidx.core.app.ActivityCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.ritmohung.srccontroller.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import org.vosk.LibVosk;
import org.vosk.LogLevel;

import es.dmoral.toasty.Toasty;


public class MainActivity extends AppCompatActivity {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    private TextView deviceName, voiceCmd, voiceResult;
    private ImageView forward, backward, left, right, stop;
    private ToggleButton recog;
    private Button file, mic;
    private VoskSR VOSK;

    // Used to handle permission request
    private static final int BLUETOOTH_PCODE = 1;
    private static final int RECORD_AUDIO_PCODE = 3;

    // BT
    private static final int CONNECT_DEVICE_SECURE_RCODE = 1;
    public static final int ENABLE_BT_RCODE = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Fragment & layout stuff
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);


        // Layout
        deviceName = (TextView) findViewById(R.id.deviceNameTextView);
        voiceCmd = (TextView) findViewById(R.id.voiceCmdTextView);
        voiceResult = (TextView) findViewById(R.id.voiceResultTextView);
        forward = (ImageView) findViewById(R.id.forImageView);
        backward = (ImageView) findViewById(R.id.backImageView);
        left = (ImageView) findViewById(R.id.leftImageView);
        right = (ImageView) findViewById(R.id.rightImageView);
        stop = (ImageView) findViewById(R.id.stopImageView);
        recog = (ToggleButton) findViewById(R.id.recogToggle);
        file = (Button) findViewById(R.id.fileModeButton);
        mic = (Button) findViewById(R.id.micModeButton);
        VOSK = new VoskSR(this, file, mic, recog, deviceName, voiceCmd, voiceResult);


        // OnClicks
        binding.fab.setOnClickListener(onFabClick);
        forward.setOnClickListener(onDirClick);
        backward.setOnClickListener(onDirClick);
        left.setOnClickListener(onDirClick);
        right.setOnClickListener(onDirClick);
        stop.setOnClickListener(onDirClick);

        forward.setOnLongClickListener(onDirLongClick);
        backward.setOnLongClickListener(onDirLongClick);
        left.setOnLongClickListener(onDirLongClick);
        right.setOnLongClickListener(onDirLongClick);
        stop.setOnLongClickListener(onDirLongClick);

        recog.setOnCheckedChangeListener((view, isChecked) -> VOSK.pause(isChecked));
        file.setOnClickListener(v -> VOSK.recognizeFile());
        mic.setOnClickListener(v -> VOSK.recognizeMicrophone());
        LibVosk.setLogLevel(LogLevel.INFO);

        // Check if user has given permission to bluetooth connection & scanning, init the model after permission is granted
        // Bluetooth adds build version check since only Android 12+ device needed
        String[] recordPermission = {Manifest.permission.RECORD_AUDIO};
        String[] btPermissions = new String[] {Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN};
        if(!hasPermission(this, recordPermission))
            ActivityCompat.requestPermissions(this, recordPermission, RECORD_AUDIO_PCODE);
        else {
            VOSK.init();
            Toasty.success(MainActivity.this, "VoskSR initialized", Toast.LENGTH_SHORT, true).show();
        }
        if(!hasPermission(this, btPermissions) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            ActivityCompat.requestPermissions(this, btPermissions, BLUETOOTH_PCODE);
        else {
            VOSK.BTTX.init();
            Toasty.success(MainActivity.this, "BT initialized", Toast.LENGTH_SHORT, true).show();
        }



        // New method: startActivityForResult is deprecated
        /*
        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                new ActivityResultCallback<ActivityResult>() {
                    @Override
                    public void onActivityResult(ActivityResult result) {
                        if (result.getResultCode() == Activity.RESULT_OK) {
                            Intent data = result.getData();
                            // Handle the Intent
                        }
                    }
                });
         */
    }



    // Override: Layout related
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent s = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(s);
            return true;
        }
        else if(id == R.id.action_disconnect) {
            if(VOSK.BTTX.CONNECTED)
                if(VOSK.BTTX.reset())
                    Toasty.success(MainActivity.this, R.string.BT_CLOSED, Toast.LENGTH_SHORT, true).show();
                else
                    Toasty.warning(MainActivity.this, R.string.BT_UNSAFE_CLOSED, Toast.LENGTH_SHORT, true).show();
        }

        return super.onOptionsItemSelected(item);
    }
    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }

    // Override: Handle result codes from BT list activity
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case ENABLE_BT_RCODE:
                if(resultCode == Activity.RESULT_OK) {
                    Log.d("BT: ", "BT enabled");
                    Toasty.info(MainActivity.this, R.string.BT_ON, Toast.LENGTH_SHORT, true).show();
                }
                break;

            case CONNECT_DEVICE_SECURE_RCODE:
                if(resultCode == Activity.RESULT_OK) {
                    Bundle extras = data.getExtras();
                    if(extras == null) return;

                    String name = extras.getString(BTListActivity.EXTRA_DEVICE_NAME);
                    String address = extras.getString(BTListActivity.EXTRA_DEVICE_ADDRESS);

                    if(VOSK.BTTX.connect(address)) {
                        deviceName.setText(name);
                        Toasty.info(getApplicationContext(), "Connected to: " + name + "\n" + address, Toast.LENGTH_SHORT, true).show();
                    }
                    else {
                        VOSK.BTTX.reset();
                        Toasty.error(MainActivity.this, R.string.BT_FAIL, Toast.LENGTH_SHORT, true).show();
                    }
                }
                break;
        }
    }
    // Override: Handle audio record permission, initializes VOSK after permission granted
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == BLUETOOTH_PCODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                VOSK.BTTX.init();
                Toasty.success(MainActivity.this, "BT initialized", Toast.LENGTH_SHORT, true).show();
            }
            else
                Toasty.error(MainActivity.this, "Permission is needed", Toast.LENGTH_SHORT, true).show();
                finish();
        }
        else if(requestCode == RECORD_AUDIO_PCODE) {
            // Recognizer initialization is time-consuming and involves IO
            // Therefore execute in async task
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                VOSK.init();
                Toasty.success(MainActivity.this, "VoskSR initialized", Toast.LENGTH_SHORT, true).show();
            }
            else
                Toasty.error(MainActivity.this, "Permission is needed", Toast.LENGTH_SHORT, true).show();
                finish();
        }
    }
    // Override: Handle speech service destroy
    @Override
    public void onDestroy() {
        super.onDestroy();

        if(VOSK.BTTX.reset())
            Toasty.success(MainActivity.this, R.string.BT_CLOSED, Toast.LENGTH_SHORT, true).show();
        else
            Toasty.warning(MainActivity.this, R.string.BT_UNSAFE_CLOSED, Toast.LENGTH_SHORT, true).show();

        if (VOSK.speechService != null) {
            VOSK.speechService.stop();
            VOSK.speechService.shutdown();
        }
        if (VOSK.speechStreamService != null)
            VOSK.speechStreamService.stop();
    }

    // Permission Check
    public boolean hasPermission(Context context, String... PERMISSIONS) {
        if(context != null && PERMISSIONS != null) {
            for(String permission: PERMISSIONS) {
                if(ActivityCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED)
                    return false;
            }
        }
        return true;
    }




    // OnClickListener: Opens bluetooth device list (floating action button)
    public View.OnClickListener onFabClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            Intent s = new Intent(MainActivity.this, BTListActivity.class);
            startActivityForResult(s, CONNECT_DEVICE_SECURE_RCODE);
        }
    };

    // OnClickListener: Direction buttons - short click
    public View.OnClickListener onDirClick = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // c = 'S';
            // VOSK.BTTX.c = c;
        }
    };

    // OnLongClickListener: Direction buttons - long press
    public View.OnLongClickListener onDirLongClick = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(View view) {
            /*
            switch((view.getId())) {
                case R.id.forImageView:
                    c = 'F';
                    break;
                case R.id.leftImageView:
                    c = 'L';
                    break;
                case R.id.rightImageView:
                    c = 'R';
                    break;
                case R.id.backImageView:
                    c = 'B';
                    break;
                default:
                    c = 'S';
                    break;
            }
            VOSK.BTTX.c = c;
            deviceName.setText(Character.toString(c));
            return false;
            */
            return true;
        }
    };
}