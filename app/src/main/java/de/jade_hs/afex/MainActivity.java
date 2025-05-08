package de.jade_hs.afex;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.ServiceConnection;
import android.Manifest;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.jade_hs.afex.AcousticFeatureExtraction.MessageListener;
import de.jade_hs.afex.Tools.AudioFileIO;

public class MainActivity extends AppCompatActivity implements MessageListener {

    Context context = this;
    FloatingActionButton fabStart;
    TextView textState, textDevice, textVAD, textClass;
    ControlService controlService;
    boolean isBound = false;

    private String[] necessaryPermissions = {
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.INTERNET,
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        AndroidThreeTen.init(this);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                startActivity(intent);
            }
        }

        // check if configuration is present (1st start),
        // create one if necessary
        defaultConfiguration();

        setupUI();
    }

    @Override
    protected void onStart() {
        super.onStart();

        //checkPermission();

        Intent intent = new Intent(this, ControlService.class);

        if (!isServiceRunning())
            startService(intent);

        bindService(intent, connection, Context.BIND_AUTO_CREATE);

    }

    @Override
    protected void onStop() {
        super.onStop();

        if (isBound)
            unbindService(connection);
    }

    @Override
    public void onMessage(String tag, String data) {
        if ("AUDIO_DEVICE_SELECTED".equals(tag) && data instanceof String) {
            runOnUiThread(() -> {
                textDevice.setText("Current device: " + data);
            });
        } else if ("VAD".equals(tag) && data instanceof String) {
            runOnUiThread(() -> {
                textVAD.setText("VAD: " + data);
            });
        } else if ("CLASS".equals(tag) && data instanceof String) {
            runOnUiThread(() -> {
                textClass.setText("Class: " + data);
            });
        }
    }

    protected void setupUI() {

        textState = findViewById(R.id.state);
        textDevice = findViewById(R.id.device);
        textVAD = findViewById(R.id.vad);
        textClass = findViewById(R.id.classify);

        fabStart = findViewById(R.id.fabStart);
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (isBound)
                if (controlService == null || !controlService.isRunning()) {
                    Snackbar.make(view, "Starting Stage Manager", Snackbar.LENGTH_LONG).show();
                    controlService.startStageManager(context);
                } else {
                    Snackbar.make(view, "Stopping Stage Manager", Snackbar.LENGTH_LONG).show();
                    controlService.stopStageManager();
                }
            updateUI();
            }
        });
    }

    protected void defaultConfiguration() {

        File file = new File(AudioFileIO.getMainPath() + File.separator + AudioFileIO.STAGE_CONFIG);

        //if (!file.exists()) {

            InputStream in = getResources().openRawResource(R.raw.features);
            FileOutputStream out = null;

            try {

                out = new FileOutputStream(file);

                byte[] data = new byte[1024];
                int read = 0;
                System.out.print("---------> CONFIG");
                while ((read = in.read(data)) > 0) {
                    System.out.print("---------> CONFIG");
                    System.out.print(data);
                    out.write(data, 0, read);
                }

                in.close();
                out.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        //}
    }

    protected void updateUI() {

        if (controlService != null && controlService.isRunning()) {
            textState.setText("running...");
        } else {
            textState.setText("idle...");
        }
    }

    // Is ControlService already running?
    private boolean isServiceRunning() {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service :
                manager.getRunningServices(Integer.MAX_VALUE)) {
            if (ControlService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    /** Defines callbacks for service binding, passed to bindService() */
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ControlService.LocalBinder binder = (ControlService.LocalBinder) service;
            controlService = binder.getService();
            controlService.setMessageListener(MainActivity.this);
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };


    public void checkPermission() {
        for (String permission : necessaryPermissions) {
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{permission}, 1);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 1: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkPermission();
                } else {
                    Toast.makeText(this, "All Permissions must be granted", Toast.LENGTH_LONG).show();
                    this.finish();
                }
                return;
            }
        }
    }

}
