package com.pmr.admin;

import android.content.Context;
import android.media.AudioRecord;
import android.media.AudioTrack;
import android.util.Log;

public class AudioEngine {
    private static final int BUF_ELEMENTS = 320;
    private static final int SAMPLE_RATE_16K = 16000;
    private static final int SAMPLE_RATE_8K = 8000;
    private static final float VOLUME_BOOST = 1.7f;
    private static final int SLOTS_PER_FORMAT = 20;

    private final Context appCtx;
    private final PmrSocket pmrSocket;
    private final G711Ua g711 = new G711Ua();
    private AudioTrack[] tracks = new AudioTrack[40];
    private AudioRecord recorder = null;
    private Thread recorderThread = null;
    private volatile boolean isRecording = false;
    private volatile boolean isPlaying = false;

    public AudioEngine(Context ctx, PmrSocket sock) {
        this.appCtx = ctx;
        this.pmrSocket = sock;
    }

    public void startPlaying() {
        if (this.isPlaying) return;
        for (int i = 0; i < SLOTS_PER_FORMAT; i++) {
            if (tracks[i] == null) {
                tracks[i] = new AudioTrack(3, SAMPLE_RATE_16K, 4, 2, AudioTrack.getMinBufferSize(SAMPLE_RATE_16K, 4, 2), 1);
                try { tracks[i].setVolume(VOLUME_BOOST); } catch (Exception ignored) {}
            }
        }
        for (int i = SLOTS_PER_FORMAT; i < 40; i++) {
            if (tracks[i] == null) {
                tracks[i] = new AudioTrack(3, SAMPLE_RATE_8K, 4, 2, AudioTrack.getMinBufferSize(SAMPLE_RATE_8K, 4, 2), 1);
                try { tracks[i].setVolume(VOLUME_BOOST); } catch (Exception ignored) {}
            }
        }
        this.isPlaying = true;
        AppLog.add("AudioEngine: startPlaying, volume=1.7, slots=" + tracks.length);
    }

    public void stopPlaying() {
        this.isPlaying = false;
        for (int i = 0; i < tracks.length; i++) {
            if (tracks[i] != null) {
                try { tracks[i].flush(); tracks[i].stop(); tracks[i].release(); } catch (Exception ignored) {}
                tracks[i] = null;
            }
        }
    }

    public void playG711_16k(int client, byte[] buf, int len) {
        if (isPlaying && client >= 0 && client < SLOTS_PER_FORMAT && tracks[client] != null) {
            byte[] pcm = new byte[640];
            g711.decode(buf, 4, BUF_ELEMENTS, pcm);
            try {
                tracks[client].write(pcm, 0, 640);
                if (tracks[client].getPlayState() != 3) tracks[client].play();
            } catch (Exception ignored) {}
        }
    }

    public void playG711_8k(int client, byte[] buf, int len) {
        if (isPlaying && client >= 0 && client < SLOTS_PER_FORMAT) {
            int slot = client + SLOTS_PER_FORMAT;
            if (tracks[slot] != null) {
                byte[] pcm = new byte[BUF_ELEMENTS];
                g711.decode(buf, 4, 160, pcm);
                try {
                    tracks[slot].write(pcm, 0, BUF_ELEMENTS);
                    if (tracks[slot].getPlayState() != 3) tracks[slot].play();
                } catch (Exception ignored) {}
            }
        }
    }

    public void playPCM16_16k(int client, byte[] buf, int len) {
        if (isPlaying && client >= 0 && client < SLOTS_PER_FORMAT && tracks[client] != null) {
            try {
                tracks[client].write(buf, 4, 640);
                if (tracks[client].getPlayState() != 3) tracks[client].play();
            } catch (Exception ignored) {}
        }
    }

    public void playPCM16_8k(int client, byte[] buf, int len) {
        if (isPlaying && client >= 0 && client < SLOTS_PER_FORMAT) {
            int slot = client + SLOTS_PER_FORMAT;
            if (tracks[slot] != null) {
                try {
                    tracks[slot].write(buf, 4, BUF_ELEMENTS);
                    if (tracks[slot].getPlayState() != 3) tracks[slot].play();
                } catch (Exception ignored) {}
            }
        }
    }

    public void playPCM8_16k(int client, byte[] buf, int len) {
        if (isPlaying && client >= 0 && client < SLOTS_PER_FORMAT && tracks[client] != null) {
            short[] pcm = new short[BUF_ELEMENTS];
            for (int i = 4; i < 324; i++) pcm[i - 4] = (short) (buf[i] * 256);
            byte[] out = short2byte(pcm);
            try {
                tracks[client].write(out, 0, 640);
                if (tracks[client].getPlayState() != 3) tracks[client].play();
            } catch (Exception ignored) {}
        }
    }

    public void playPCM8_8k(int client, byte[] buf, int len) {
        if (isPlaying && client >= 0 && client < SLOTS_PER_FORMAT) {
            int slot = client + SLOTS_PER_FORMAT;
            if (tracks[slot] != null) {
                short[] pcm = new short[160];
                for (int i = 4; i < 164; i++) pcm[i - 4] = (short) (buf[i] * 256);
                byte[] out = short2byte(pcm);
                try {
                    tracks[slot].write(out, 0, BUF_ELEMENTS);
                    if (tracks[slot].getPlayState() != 3) tracks[slot].play();
                } catch (Exception ignored) {}
            }
        }
    }

    public void startRecording() {
        if (isRecording) return;
        int sampleRate = 16000;
        int bufSize = 640;
        try {
            recorder = new AudioRecord(1, sampleRate, 16, 2, bufSize * 2);
            recorder.startRecording();
            isRecording = true;
            recorderThread = new Thread(() -> recordLoop(sampleRate), "pmr-rec");
            recorderThread.start();
            AppLog.add("AudioEngine: запись стартовала, rate=" + sampleRate);
        } catch (Exception e) {
            AppLog.add("AudioEngine: ошибка записи — " + e);
        }
    }

    public void stopRecording() {
        isRecording = false;
        if (recorder != null) {
            try { recorder.stop(); recorder.release(); } catch (Exception ignored) {}
            recorder = null;
        }
        recorderThread = null;
        AppLog.add("AudioEngine: запись остановлена");
    }

    private void recordLoop(int sampleRate) {
        byte[] pcm16 = new byte[640];
        byte[] g711buf = new byte[BUF_ELEMENTS];
        while (isRecording) {
            try {
                int read = recorder.read(pcm16, 0, pcm16.length);
                if (read > 0) {
                    g711.encode(pcm16, 0, read, g711buf);
                    int payloadLen = read / 2;
                    byte[] payload = new byte[payloadLen];
                    System.arraycopy(g711buf, 0, payload, 0, payloadLen);
                    
                    int secret = pmrSocket != null ? pmrSocket.getKanalSecretInstance() : 0;
                    byte[] packet = new byte[payloadLen + 6];
                    packet[0] = 22;
                    packet[1] = 0;
                    packet[2] = (byte) (PmrSocket.Priznak_pmr & 255);
                    packet[3] = (byte) ((PmrSocket.Priznak_pmr >> 8) & 255);
                    packet[4] = (byte) (secret & 255);
                    packet[5] = (byte) ((secret >> 8) & 255);
                    System.arraycopy(payload, 0, packet, 6, payloadLen);
                    
                    byte[] mainPacket = new byte[payloadLen + 4];
                    mainPacket[0] = 22;
                    mainPacket[1] = 0;
                    mainPacket[2] = (byte) (PmrSocket.Priznak_pmr & 255);
                    mainPacket[3] = (byte) ((PmrSocket.Priznak_pmr >> 8) & 255);
                    System.arraycopy(payload, 0, mainPacket, 4, payloadLen);
                    
                    if (pmrSocket != null) pmrSocket.sendVoice(mainPacket, packet);
                }
            } catch (Exception e) {
                return;
            }
        }
    }

    private static byte[] short2byte(short[] sArr) {
        byte[] bArr = new byte[sArr.length * 2];
        for (int i = 0; i < sArr.length; i++) {
            bArr[i * 2] = (byte) (sArr[i] & 255);
            bArr[i * 2 + 1] = (byte) (sArr[i] >> 8);
        }
        return bArr;
    }
}