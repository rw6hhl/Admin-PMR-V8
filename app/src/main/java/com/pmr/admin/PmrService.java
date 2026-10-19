package com.pmr.admin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class PmrService extends Service {
    private static final String CHANNEL_ID = "PMR_Service_Channel";
    private static PmrSocket pmrSocket;
    private AudioEngine audioEngine;

    public static PmrSocket getPmrSocket() { return pmrSocket; }

    public static void setChanListListener(PmrSocket.OnChanListListener listener) {
        PmrSocket.setChanListListener(listener);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("PMR Radio")
                .setContentText("Работа в фоне...")
                .setSmallIcon(R.drawable.wktpaint1icon)
                .build();
        startForeground(1, notification);

        pmrSocket = new PmrSocket(this, clientId -> {
            Intent voiceIntent = new Intent("PMR_VOICE_RECEIVED");
            voiceIntent.putExtra("clientId", clientId);
            sendBroadcast(voiceIntent);
        });
        
        audioEngine = new AudioEngine(this, pmrSocket);
        
        pmrSocket.setAudioEngine(audioEngine);
        
        pmrSocket.start();
        audioEngine.startPlaying();

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (audioEngine != null) {
            audioEngine.stopPlaying();
            audioEngine.stopRecording();
        }
        if (pmrSocket != null) pmrSocket.stop();
        pmrSocket = null;
    }

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