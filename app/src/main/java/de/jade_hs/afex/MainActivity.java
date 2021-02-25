package de.jade_hs.afex;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.jakewharton.threetenabp.AndroidThreeTen;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import de.jade_hs.afex.Tools.AudioFileIO;

public class MainActivity extends AppCompatActivity {

    FloatingActionButton fabStart;
    TextView textState;
    ControlService controlService;
    boolean isBound = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);

        AndroidThreeTen.init(this);

        // check if configuration is present (1st start),
        // create one if necessary
        defaultConfiguration();

        setupUI();
    }

    @Override
    protected void onStart() {
        super.onStart();

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

    protected void setupUI() {

        textState = (TextView) findViewById(R.id.state);

        fabStart = findViewById(R.id.fabStart);
        fabStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            if (isBound)
                if (controlService == null || !controlService.isRunning()) {
                    Snackbar.make(view, "Starting Stage Manager", Snackbar.LENGTH_LONG).show();
                    controlService.startStageManager();
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

        if (!file.exists()) {

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
        }
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
            isBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            isBound = false;
        }
    };

}
