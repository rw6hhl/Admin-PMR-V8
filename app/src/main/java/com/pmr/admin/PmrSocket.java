package com.pmr.admin;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

public class PmrSocket {
    private static final String TAG = "PmrSocket";
    private DatagramSocket socket;
    private InetAddress serverAddr;
    private boolean isRunning = false;
    private Thread receiveThread;
    private Thread pingThread;
    private AudioEngine audioEngine;

    private SharedPreferences prefs;
    public static int Priznak_pmr = 26000;
    private int myMailIndex = 51953;
    private int portPrm = 5254;
    private int portPrd = 16000;
    private int kanalPrd = 13;
    private int kanalSecret = 0;
    private int clientNumber = -1;

    public interface OnVoiceListener {
        void onVoiceReceived(int clientId);
    }
    private OnVoiceListener voiceListener;

    public static class ChanEntry {
        public int id;
        public String ip;
        public boolean ban;
        public ChanEntry(int id, String ip, boolean ban) {
            this.id = id; this.ip = ip; this.ban = ban;
        }
    }

    public interface OnChanListListener {
        void onChanListReceived(List<ChanEntry> entries);
    }
    private static OnChanListListener chanListListener;

    public PmrSocket(Context context, OnVoiceListener listener) {
        this.voiceListener = listener;
        this.prefs = context.getSharedPreferences("pmr_prefs", Context.MODE_PRIVATE);
        loadSettings();
        try {
            this.serverAddr = InetAddress.getByName("185.221.154.39");
            this.socket = new DatagramSocket(portPrm);
            this.socket.setSoTimeout(1000);
            AppLog.add("Сокет открыт: IP=185.221.154.39, порт=" + portPrm + ", секрет=" + kanalSecret);
        } catch (Exception e) {
            Log.e(TAG, "Error init socket: " + e.getMessage());
            AppLog.add("Ошибка сокета: " + e.getMessage());
        }
    }

    public static void setChanListListener(OnChanListListener listener) {
        chanListListener = listener;
    }

    private void loadSettings() {
        Priznak_pmr = 26000;
        myMailIndex = 51953;
        portPrm = 5254;
        portPrd = 16000;

        kanalSecret = (myMailIndex & 0xFFFFFFF0) >> 4;

        int myPChannel = prefs.getInt("current_channel", 1);
        if (myPChannel == 0) kanalPrd = 0;
        else kanalPrd = ((myMailIndex & 0xF) * 8) + myPChannel;

        AppLog.add("Настройки: ID=" + Priznak_pmr + ", Mail=" + myMailIndex + ", Канал=" + kanalPrd + ", Секрет=" + kanalSecret + ", Порт=" + portPrd);
    }

    public void reloadFromPrefs(Context context) {
        SharedPreferences sp = context.getSharedPreferences("pmr_prefs", Context.MODE_PRIVATE);
        try {
            myMailIndex = Integer.parseInt(sp.getString("MyMailIndex", "51953"));
        } catch (Exception ignored) {}
        try {
            Priznak_pmr = Integer.parseInt(sp.getString("Priznak_pmr", "26000"));
        } catch (Exception ignored) {}
        portPrd = sp.getInt("PORT_prd", 16000);
        portPrm = sp.getInt("PORT_prm", 5254);
        try {
            String ip = sp.getString("ip_server", "185.221.154.39");
            serverAddr = InetAddress.getByName(ip);
        } catch (Exception ignored) {}
        int myPChannel = sp.getInt("current_channel", 1);
        if (myPChannel == 0) kanalPrd = 0;
        else kanalPrd = ((myMailIndex & 0xF) * 8) + myPChannel;
        kanalSecret = (myMailIndex & 0xFFFFFFF0) >> 4;
        AppLog.add("PmrSocket.reload: mail=" + myMailIndex + ", ch=" + myPChannel + ", priznak=" + Priznak_pmr + ", kanal=" + kanalPrd + ", secret=" + kanalSecret);
    }

    public void recreateSocket(Context context) {
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
        reloadFromPrefs(context);
        try {
            this.socket = new DatagramSocket(portPrm);
            this.socket.setSoTimeout(1000);
            AppLog.add("Сокет пересоздан: порт=" + portPrm + ", канал=" + kanalPrd);
        } catch (Exception e) {
            Log.e(TAG, "Error recreate socket: " + e.getMessage());
            AppLog.add("Ошибка пересоздания сокета: " + e.getMessage());
            return;
        }
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

    public int getClientNumber() {
        return clientNumber;
    }

    public int getKanalSecretInstance() {
        return kanalSecret;
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
        // Автозапрос cmd=234 убран по требованию (A1.8). Запрос — при открытии списка.
    }

    private void startPingThread() {
        pingThread = new Thread(() -> {
            while (isRunning) {
                try {
                    byte[] ping = new byte[4];
                    ping[0] = 7;
                    ping[1] = 0;
                    ping[2] = (byte) (kanalSecret & 0xFF);
                    ping[3] = (byte) ((kanalSecret >> 8) & 0xFF);
                    socket.send(new DatagramPacket(ping, 4, serverAddr, portPrd + kanalPrd));

                    Thread.sleep(1000);
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
        int client = (data[2] & 0xFF) | ((data[3] & 0xFF) << 8);

        if (kanal != kanalPrd && kanalPrd != 0) return;

        if (command != 22) {
            AppLog.add("← Входящий: cmd=" + command + ", kanal=" + kanal + ", client=" + client + ", size=" + data.length);
        }

        if (command == 22 && data.length == 324) {
            if (audioEngine != null) audioEngine.pushToRingBuffer(data);
            if (voiceListener != null) voiceListener.onVoiceReceived(client);
        } else if (command == 110) {
            clientNumber = client;
            // При изменении количества — запрос списка (как в admin.c, case 'n')
            requestChanList();
        } else if (command == 234) {
            parseChanList(data);
        }
    }

    private void parseChanList(byte[] data) {
        List<ChanEntry> entries = new ArrayList<>();
        int payload = data.length - 4;
        int count = payload / 13;
        for (int i = 0; i < count; i++) {
            int offset = 4 + (i * 13);
            if (offset + 12 >= data.length) break;
            // Структура CLI из admin.c: i(2), cod(2), Id(2), port(2), ip(4), ban(1) = 13 байт
            int id = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8);
            int ip0 = data[offset + 8] & 0xFF;
            int ip1 = data[offset + 9] & 0xFF;
            int ip2 = data[offset + 10] & 0xFF;
            int ip3 = data[offset + 11] & 0xFF;
            String ip = ip0 + "." + ip1 + "." + ip2 + "." + ip3;
            boolean ban = (data[offset + 12] & 0xFF) != 0;
            entries.add(new ChanEntry(id, ip, ban));
        }
        if (chanListListener != null) {
            chanListListener.onChanListReceived(entries);
        }
    }

    public void requestChanList() {
        if (socket == null || socket.isClosed()) return;
        try {
            byte[] cmd = new byte[4];
            cmd[0] = (byte) 234;
            cmd[1] = 0;
            socket.send(new DatagramPacket(cmd, 4, serverAddr, portPrd + kanalPrd));
            AppLog.add("→ Отправлено: cmd=234 (запрос списка) на порт " + (portPrd + kanalPrd));
        } catch (IOException e) {
            AppLog.add("Ошибка отправки списка: " + e.getMessage());
        }
    }

    public void sendBan(int targetId) {
        byte[] ban = new byte[4];
        ban[0] = (byte) 222;
        ban[1] = 0;
        ban[2] = (byte) (targetId & 0xFF);
        ban[3] = (byte) ((targetId >> 8) & 0xFF);
        try {
            socket.send(new DatagramPacket(ban, 4, serverAddr, portPrd + kanalPrd));
            AppLog.add("→ Отправлено: БАН ID=" + targetId);
        } catch (IOException e) {
            AppLog.add("Ошибка бана: " + e.getMessage());
        }
    }

    public void sendUnban(int targetId) {
        sendBan(targetId);
        AppLog.add("→ Отправлено: РАЗБАН (повтор 222) ID=" + targetId);
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) socket.close();
        AppLog.add("Работа сокета остановлена");
    }

    public void sendVoice(byte[] mainPacket, byte[] reservePacket) {
        if (socket == null || socket.isClosed()) return;
        try {
            if (mainPacket != null) {
                socket.send(new DatagramPacket(mainPacket, mainPacket.length, serverAddr, portPrd + kanalPrd));
            }
        } catch (IOException e) {
            AppLog.add("Ошибка отправки голоса: " + e.getMessage());
        }
    }
}