package de.jade_hs.afex;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import de.jade_hs.afex.AcousticFeatureExtraction.StageManager;

public class ControlService extends Service {

    public static final String LOG = "ControlService";

    public static final int NOTIFICATION_ID = 1;

    public final IBinder binder  = new LocalBinder();

    StageManager stageManager = new StageManager(this);

    /**
     * Binder returns enclosing ControlService instance
     */

    public class LocalBinder extends Binder {
        ControlService getService() {
            return ControlService.this;
        }
    }

    /**
     * Lifecycle callbacks
     */

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        setNotification();

        Log.d(LOG, "Service started");

        return START_STICKY; // mode to explicitly start and stop service
    }

    @Override
    public IBinder onBind(Intent intent) {
        Log.d(LOG, "Service bound");
        return binder;
    }

    @Override
    public void onDestroy() {

        clearNotification();
        Log.d(LOG, "Service destroyed");
        super.onDestroy();
    }


    /**
     * Methods for clients
     * */

    public void startStageManager() {
//        stageManager = new StageManager();
        stageManager.start();

    }

    public void stopStageManager() {
        stageManager.stop();
    }

    public boolean isRunning() {
        return stageManager.isRunning;
    }

    /**
     * Additional setup & configuration
     */

    public void setNotification() {

        String NOTIFICATION_CHANEL_ID = "AFE";

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANEL_ID,
                    NOTIFICATION_CHANEL_ID, NotificationManager.IMPORTANCE_NONE);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            assert manager != null;
            manager.createNotificationChannel(channel);
        }

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder( this, NOTIFICATION_CHANEL_ID);
        Notification notification = notificationBuilder
                .setOngoing(true)
                .setPriority(NotificationManager.IMPORTANCE_MIN)
                .setCategory(Notification.CATEGORY_SERVICE)
                .setAutoCancel( false )
                .setWhen( System.currentTimeMillis() )
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    public void clearNotification() {

        NotificationManager mNotificationManager = (NotificationManager) getSystemService( Context.NOTIFICATION_SERVICE );
        mNotificationManager.cancel(NOTIFICATION_ID);
    }

}
