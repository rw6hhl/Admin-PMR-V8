package com.pmr.admin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class PmrService extends Service {
    private static final String CHANNEL_ID = "PMR_Service_Channel";
    private static PmrSocket pmrSocket;
    private AudioEngine audioEngine;

    public static PmrSocket getPmrSocket() { return pmrSocket; }

    private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("PMR_START_RECORDING".equals(action)) {
                if (audioEngine != null) audioEngine.startRecording();
            } else if ("PMR_STOP_RECORDING".equals(action)) {
                if (audioEngine != null) audioEngine.stopRecording();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        IntentFilter filter = new IntentFilter();
        filter.addAction("PMR_START_RECORDING");
        filter.addAction("PMR_STOP_RECORDING");
        registerReceiver(controlReceiver, filter);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PMR Radio")
                .setContentText("Работа в фоне...")
                .setSmallIcon(R.drawable.wktpaint1icon)
                .build();
        startForeground(1, notification);

        audioEngine = new AudioEngine(null);
        pmrSocket = new PmrSocket(this, clientId -> {
            Intent voiceIntent = new Intent("PMR_VOICE_RECEIVED");
            voiceIntent.putExtra("clientId", clientId);
            sendBroadcast(voiceIntent);
        });
        
        pmrSocket.setAudioEngine(audioEngine);
        audioEngine.setSocket(pmrSocket);
        
        pmrSocket.start();
        audioEngine.startPlaying();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(controlReceiver);
        if (audioEngine != null) {
            audioEngine.stopPlaying();
            audioEngine.stopRecording();
        }
        if (pmrSocket != null) pmrSocket.stop();
        pmrSocket = null;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID, "PMR Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(serviceChannel);
        }
    }
}