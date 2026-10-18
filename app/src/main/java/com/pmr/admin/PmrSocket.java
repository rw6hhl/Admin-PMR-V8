package com.pmr.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class PmrSocket {
    private static final String TAG = "PmrSocket";
    private DatagramSocket socket;
    private InetAddress serverAddr;
    private int serverPort;
    private boolean isRunning = false;
    private Thread receiveThread;
    private Thread pingThread;
    private AudioEngine audioEngine;
    
    private SharedPreferences prefs;
    private int priznakPmr = 26001;
    private int myMailIndex = 111111;
    private int portPrm = 5323;
    private int portPrd = 16000;
    private int kanalPrd = 13;

    public interface OnVoiceListener {
        void onVoiceReceived(int clientId);
    }
    private OnVoiceListener voiceListener;

    public PmrSocket(Context context, OnVoiceListener listener) {
        this.voiceListener = listener;
        this.prefs = context.getSharedPreferences("pmr_prefs", Context.MODE_PRIVATE);
        loadSettings();
        try {
            this.serverAddr = InetAddress.getByName(prefs.getString("ip_server", "185.221.154.39"));
            this.socket = new DatagramSocket(portPrm);
            this.socket.setSoTimeout(1000);
        } catch (Exception e) {
            Log.e(TAG, "Error init socket: " + e.getMessage());
        }
    }

    private void loadSettings() {
        priznakPmr = Integer.parseInt(prefs.getString("Priznak_pmr", "26001"));
        myMailIndex = Integer.parseInt(prefs.getString("MyMailIndex", "111111"));
        portPrm = prefs.getInt("PORT_prm", 5323);
        portPrd = prefs.getInt("PORT_prd", 16000);
        
        int myPChannel = prefs.getInt("current_channel", 1);
        if (myPChannel == 0) kanalPrd = 0;
        else kanalPrd = ((myMailIndex & 0xF) * 8) + myPChannel;
    }

    public void setAudioEngine(AudioEngine audioEngine) {
        this.audioEngine = audioEngine;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        
        receiveThread = new Thread(() -> {
            byte[] buffer = new byte[2048];
            while (isRunning) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);
                    byte[] data = new byte[packet.getLength()];
                    System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                    parsePacket(data);
                } catch (Exception ignored) {}
            }
        }, "PMR-ReceiveThread");
        receiveThread.start();
        
        startPingThread();
    }

    private void startPingThread() {
        pingThread = new Thread(() -> {
            int cikl = 0;
            while (isRunning) {
                try {
                    // cmd=7 (секрет) или cmd=0
                    byte[] ping = new byte[4];
                    ping[0] = 7; // command
                    ping[1] = 13; // kanal
                    ping[2] = 0; // client low
                    ping[3] = 0; // client high
                    socket.send(new DatagramPacket(ping, 4, serverAddr, portPrd + kanalPrd));
                    
                    Thread.sleep(1000);
                    cikl++;
                    
                    if (cikl > 3) {
                        // cmd=0, Priznak_pmr в поле client
                        byte[] priznak = new byte[6]; // 4 байта заголовка + 2 байта данных
                        priznak[0] = 0; // command
                        priznak[1] = 13;
                        priznak[2] = (byte) (priznakPmr & 0xFF);
                        priznak[3] = (byte) ((priznakPmr >> 8) & 0xFF);
                        socket.send(new DatagramPacket(priznak, 6, serverAddr, portPrd + kanalPrd));
                        cikl = 0;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ping error: " + e.getMessage());
                }
            }
        }, "PMR-PingThread");
        pingThread.start();
    }

    private void parsePacket(byte[] data) {
        if (data.length < 4) return;
        int command = data[0] & 0xFF;
        int kanal = data[1] & 0xFF;
        int client = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8); // Little Endian

        if (kanal != kanalPrd && kanalPrd != 0) return;

        if (command == 22 && data.length == 324) { // Голос
            if (audioEngine != null) audioEngine.pushToRingBuffer(data);
            if (voiceListener != null) voiceListener.onVoiceReceived(client);
        } else if (command == 234) { // Список
            parseChanList(data);
        }
    }

    private void parseChanList(byte[] data) {
        int count = (data.length - 4) / 13;
        for (int i = 0; i < count; i++) {
            int offset = 4 + (i * 13);
            int id = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8);
            Log.d(TAG, "List User: " + id);
        }
    }

    public void sendBan(int targetId) {
        byte[] ban = new byte[4];
        ban[0] = (byte) 222; // ИСПРАВЛЕНО: явное приведение типа
        ban[1] = 13;
        ban[2] = (byte) (targetId & 0xFF);
        ban[3] = (byte) ((targetId >> 8) & 0xFF);
        try {
            socket.send(new DatagramPacket(ban, 4, serverAddr, portPrd + kanalPrd));
        } catch (IOException e) {
            Log.e(TAG, "Ban error");
        }
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) socket.close();
    }

    public void sendVoiceRaw(byte[] encodedG711) {
        if (socket == null || socket.isClosed()) return;
        try {
            byte[] packet = new byte[324];
            packet[0] = 22;
            packet[1] = (byte) kanalPrd;
            packet[2] = (byte) (priznakPmr & 0xFF);
            packet[3] = (byte) ((priznakPmr >> 8) & 0xFF);
            int dataLen = Math.min(encodedG711.length, 320);
            System.arraycopy(encodedG711, 0, packet, 4, dataLen);
            socket.send(new DatagramPacket(packet, 324, serverAddr, portPrd + kanalPrd));
        } catch (IOException e) {
            Log.e(TAG, "Send voice error");
        }
    }
}