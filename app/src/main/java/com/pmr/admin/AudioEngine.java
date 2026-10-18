package com.pmr.admin;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.media.MediaRecorder;
import android.util.Log;

public class AudioEngine {
    private static final String TAG = "AudioEngine";
    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_MONO;
    private static final int CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;
    private static final int BUFFER_SIZE = 320;

    private AudioRecord recorder;
    private AudioTrack player;
    private boolean isRecording = false;
    private boolean isPlaying = false;
    private Thread recordThread;
    private Thread playThread;
    private PmrSocket socket;
    private G711Ua g711 = new G711Ua();

    private final byte[][] ringBuf = new byte[100][324];
    private int ringWriteIdx = 0;
    private int ringReadIdx = 0;
    private int ringCount = 0;
    private final Object ringLock = new Object();

    public AudioEngine(PmrSocket socket) {
        this.socket = socket;
        int minBufIn = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT);
        int minBufOut = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT);

        try {
            recorder = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, CHANNEL_CONFIG_IN, AUDIO_FORMAT, Math.max(minBufIn, BUFFER_SIZE * 2));
            player = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE, CHANNEL_CONFIG_OUT, AUDIO_FORMAT, Math.max(minBufOut, BUFFER_SIZE * 4), AudioTrack.MODE_STREAM);
        } catch (Exception e) {
            Log.e(TAG, "Audio init error: " + e.getMessage());
        }
    }

    public void setSocket(PmrSocket socket) {
        this.socket = socket;
    }

    public void startRecording() {
        if (recorder == null || isRecording) return;
        isRecording = true;
        recorder.startRecording();

        recordThread = new Thread(() -> {
            byte[] buffer = new byte[BUFFER_SIZE * 2];
            byte[] encoded = new byte[BUFFER_SIZE];
            while (isRecording) {
                int read = recorder.read(buffer, 0, buffer.length);
                if (read > 0 && socket != null) {
                    g711.encode(buffer, 0, read, encoded);
                    socket.sendVoiceRaw(encoded);
                }
            }
        }, "AudioRecordThread");
        recordThread.start();
    }

    public void stopRecording() {
        if (!isRecording) return;
        isRecording = false;
        if (recorder != null) {
            try { recorder.stop(); } catch (Exception ignored) {}
        }
    }

    public void startPlaying() {
        if (player == null || isPlaying) return;
        isPlaying = true;
        player.play();

        playThread = new Thread(() -> {
            byte[] pcmData = new byte[BUFFER_SIZE * 2];
            while (isPlaying) {
                synchronized (ringLock) {
                    if (ringCount > 0) {
                        byte[] packet = ringBuf[ringReadIdx];
                        ringReadIdx = (ringReadIdx + 1) % 100;
                        ringCount--;
                        g711.decode(packet, 4, 320, pcmData);
                        player.write(pcmData, 0, pcmData.length);
                    }
                }
                try { Thread.sleep(10); } catch (InterruptedException ignored) {}
            }
        }, "AudioPlayThread");
        playThread.start();
    }

    public void stopPlaying() {
        if (!isPlaying) return;
        isPlaying = false;
        if (player != null) {
            try { player.stop(); } catch (Exception ignored) {}
        }
    }

    public void pushToRingBuffer(byte[] packet) {
        synchronized (ringLock) {
            ringBuf[ringWriteIdx] = packet;
            ringWriteIdx = (ringWriteIdx + 1) % 100;
            ringCount++;
        }
    }
}