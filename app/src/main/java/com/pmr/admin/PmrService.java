package com.pmr.admin;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;

public class PmrService extends Service {
    public static final String CHANNEL_ID = "pmr_admin_ch";
    public static final int NOTIF_ID = 1;

    public static PmrSocket pmrSocket;
    public static AudioEngine audioEngine;

    public static PmrSocket getPmrSocket() {
        return pmrSocket;
    }

    public static void setChanListListener(PmrSocket.OnChanListListener listener) {
        PmrSocket.setChanListListener(listener);
    }

    private BroadcastReceiver controlReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("PMR_START_RECORDING".equals(action)) {
                AppLog.add("PmrService: PMR_START_RECORDING → startRecording");
                if (audioEngine != null) audioEngine.startRecording();
            } else if ("PMR_STOP_RECORDING".equals(action)) {
                AppLog.add("PmrService: PMR_STOP_RECORDING → stopRecording");
                if (audioEngine != null) audioEngine.stopRecording();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.add("PmrService: onCreate — старт V9.39");

        IntentFilter filter = new IntentFilter();
        filter.addAction("PMR_START_RECORDING");
        filter.addAction("PMR_STOP_RECORDING");
        registerReceiver(controlReceiver, filter);

        pmrSocket = new PmrSocket(getApplicationContext(), null);
        audioEngine = new AudioEngine(getApplicationContext(), pmrSocket);
        pmrSocket.setAudioEngine(audioEngine);
        audioEngine.startPlaying();
        pmrSocket.start();

        createChannel();
        startForeground(NOTIF_ID, buildNotification());
        AppLog.add("PmrService: служба запущена, audioEngine и pmrSocket созданы");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.add("PmrService: onStartCommand START_STICKY");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        AppLog.add("PmrService: onDestroy");
        try { unregisterReceiver(controlReceiver); } catch (Exception ignored) {}
        if (audioEngine != null) {
            try { audioEngine.stopRecording(); } catch (Exception ignored) {}
            try { audioEngine.stopPlaying(); } catch (Exception ignored) {}
        }
        if (pmrSocket != null) pmrSocket.stop();
        pmrSocket = null;
        audioEngine = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    getString(R.string.app_name),
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager nm = getSystemService(NotificationManager.class);
            if (nm != null) nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification() {
        Intent i = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                this, 0, i, PendingIntent.FLAG_IMMUTABLE);
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle(getString(R.string.app_name))
                .setContentText("Работа в фоне...")
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }
}