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
    private int portPrm = 5253;
    private int portPrd = 16000;
    private int kanalPrd = 13;
    private int kanalSecret = 0;
    private int clientNumber = -1;
    private int pingCount = 0;

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
    private static List<ChanEntry> lastChanList = new ArrayList<>();

    public PmrSocket(Context context, OnVoiceListener listener) {
        this.voiceListener = listener;
        this.prefs = context.getSharedPreferences("pmr_prefs", Context.MODE_PRIVATE);
        loadSettings();
        try {
            this.serverAddr = InetAddress.getByName("185.221.154.39");
            this.socket = new DatagramSocket(portPrm);
            this.socket.setSoTimeout(1000);
            AppLog.add("PmrSocket: Сокет открыт, порт=" + portPrm + ", IP=" + serverAddr.getHostAddress());
        } catch (Exception e) {
            Log.e(TAG, "Error init socket: " + e.getMessage());
            AppLog.add("PmrSocket: ОШИБКА сокета — " + e.getMessage());
        }
    }

    public void setVoiceListener(OnVoiceListener listener) {
        this.voiceListener = listener;
        AppLog.add("PmrSocket: setVoiceListener " + (listener == null ? "null" : listener.getClass().getSimpleName()));
    }

    public static void setChanListListener(OnChanListListener listener) {
        chanListListener = listener;
        AppLog.add("PmrSocket: setChanListListener " + (listener == null ? "null" : listener.getClass().getSimpleName())
                + ", lastChanList.size=" + lastChanList.size());
        if (listener != null && !lastChanList.isEmpty()) {
            AppLog.add("PmrSocket: отдаю lastChanList (size=" + lastChanList.size() + ") в Activity сразу");
            listener.onChanListReceived(lastChanList);
        }
    }

    private void loadSettings() {
        try {
            myMailIndex = Integer.parseInt(prefs.getString("MyMailIndex", "51953"));
        } catch (Exception ignored) {}
        try {
            Priznak_pmr = Integer.parseInt(prefs.getString("Priznak_pmr", "26000"));
        } catch (Exception ignored) {}
        portPrm = prefs.getInt("PORT_prm", 5253);
        portPrd = prefs.getInt("PORT_prd", 16000);

        kanalSecret = (myMailIndex & 0xFFFFFFF0) >> 4;

        int myPChannel = prefs.getInt("current_channel", 4);
        if (myPChannel == 0) kanalPrd = 0;
        else kanalPrd = ((myMailIndex & 0xF) * 8) + myPChannel;

        AppLog.add("PmrSocket: loadSettings — ID=" + Priznak_pmr + ", Mail=" + myMailIndex
                + ", Ch=" + myPChannel + ", kanal=" + kanalPrd
                + ", secret=" + kanalSecret + ", PORT_prm=" + portPrm + ", PORT_prd=" + portPrd);
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
        portPrm = sp.getInt("PORT_prm", 5253);
        try {
            String ip = sp.getString("ip_server", "185.221.154.39");
            serverAddr = InetAddress.getByName(ip);
        } catch (Exception ignored) {}
        int myPChannel = sp.getInt("current_channel", 4);
        if (myPChannel == 0) kanalPrd = 0;
        else kanalPrd = ((myMailIndex & 0xF) * 8) + myPChannel;
        kanalSecret = (myMailIndex & 0xFFFFFFF0) >> 4;
        AppLog.add("PmrSocket: reloadFromPrefs — mail=" + myMailIndex + ", ch=" + myPChannel
                + ", priznak=" + Priznak_pmr + ", kanal=" + kanalPrd
                + ", secret=" + kanalSecret + ", PORT_prm=" + portPrm + ", PORT_prd=" + portPrd);
    }

    public void recreateSocket(Context context) {
        AppLog.add("PmrSocket: recreateSocket — старт");
        isRunning = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (Exception ignored) {}
        if (receiveThread != null) {
            try { receiveThread.join(500); } catch (Exception ignored) {}
        }
        try { Thread.sleep(100); } catch (Exception ignored) {}
        reloadFromPrefs(context);
        try {
            this.socket = new DatagramSocket(portPrm);
            this.socket.setSoTimeout(1000);
            AppLog.add("PmrSocket: Сокет пересоздан, порт=" + portPrm + ", kanal=" + kanalPrd);
        } catch (Exception e) {
            Log.e(TAG, "Error recreate socket: " + e.getMessage());
            AppLog.add("PmrSocket: ОШИБКА пересоздания сокета — " + e.getMessage());
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
        AppLog.add("PmrSocket: start() — запуск потоков");

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
            while (isRunning) {
                try {
                    byte[] ping = new byte[4];
                    ping[0] = 7;
                    ping[1] = 0;
                    ping[2] = (byte) (kanalSecret & 0xFF);
                    ping[3] = (byte) ((kanalSecret >> 8) & 0xFF);
                    socket.send(new DatagramPacket(ping, 4, serverAddr, portPrd + kanalPrd));

                    pingCount++;
                    if (pingCount <= 5 || pingCount % 10 == 0) {
                        AppLog.add("PmrSocket: ping #" + pingCount + " → порт " + (portPrd + kanalPrd)
                                + ", secret=" + kanalSecret);
                    }

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
            AppLog.add("PmrSocket: ← cmd=" + command + ", kanal=" + kanal + ", client=" + client + ", size=" + data.length);
        }

        if (command == 22 && data.length == 324) {
            if (audioEngine != null) audioEngine.pushToRingBuffer(data);
            if (voiceListener != null) voiceListener.onVoiceReceived(client);
        } else if (command == 110) {
            if (clientNumber != client) {
                AppLog.add("PmrSocket: cmd=110 — количество изменилось " + clientNumber + " → " + client + " → requestChanList");
                clientNumber = client;
                requestChanList();
            }
        } else if (command == 234) {
            parseChanList(data);
        }
    }

    private void parseChanList(byte[] data) {
        List<ChanEntry> entries = new ArrayList<>();
        int payload = data.length - 4;
        int count = payload / 13;
        AppLog.add("PmrSocket: parseChanList — size=" + data.length + ", записей=" + count);

        for (int i = 0; i < count; i++) {
            int offset = 4 + (i * 13);
            if (offset + 12 >= data.length) break;
            int id = (data[offset + 4] & 0xFF) | ((data[offset + 5] & 0xFF) << 8);
            int ip0 = data[offset + 8] & 0xFF;
            int ip1 = data[offset + 9] & 0xFF;
            int ip2 = data[offset + 10] & 0xFF;
            int ip3 = data[offset + 11] & 0xFF;
            String ip = ip0 + "." + ip1 + "." + ip2 + "." + ip3;
            boolean ban = (data[offset + 12] & 0xFF) != 0;
            entries.add(new ChanEntry(id, ip, ban));
            AppLog.add("PmrSocket:   запись #" + i + " — id=" + id + ", ip=" + ip + ", ban=" + ban);
        }

        lastChanList = entries;

        if (chanListListener != null) {
            chanListListener.onChanListReceived(entries);
        } else {
            AppLog.add("PmrSocket: chanListListener == null, список сохранён в lastChanList (size=" + entries.size() + ")");
        }
    }

    public void requestChanList() {
        if (socket == null || socket.isClosed()) {
            AppLog.add("PmrSocket: requestChanList — сокет null/closed, пропуск");
            return;
        }
        try {
            byte[] cmd = new byte[4];
            cmd[0] = (byte) 234;
            cmd[1] = 13;
            cmd[2] = 0;
            cmd[3] = 0;
            int listPort = portPrd + 13;
            socket.send(new DatagramPacket(cmd, 4, serverAddr, listPort));
            AppLog.add("PmrSocket: → cmd=234 отправлен, порт=" + listPort + ", kanal=13");
        } catch (IOException e) {
            AppLog.add("PmrSocket: ОШИБКА cmd=234 — " + e.getMessage());
        }
    }

    public void sendBan(int targetId) {
        if (socket == null || socket.isClosed()) {
            AppLog.add("PmrSocket: sendBan — сокет null/closed, пропуск (id=" + targetId + ")");
            return;
        }
        byte[] ban = new byte[4];
        ban[0] = (byte) 222;
        ban[1] = 0;
        ban[2] = (byte) (targetId & 0xFF);
        ban[3] = (byte) ((targetId >> 8) & 0xFF);
        try {
            socket.send(new DatagramPacket(ban, 4, serverAddr, portPrd + kanalPrd));
            AppLog.add("PmrSocket: → cmd=222 (БАН) id=" + targetId + ", порт=" + (portPrd + kanalPrd));
        } catch (IOException e) {
            AppLog.add("PmrSocket: ОШИБКА cmd=222 — " + e.getMessage());
        }
    }

    public void sendUnban(int targetId) {
        sendBan(targetId);
        AppLog.add("PmrSocket: → РАЗБАН id=" + targetId + " (повтор 222)");
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) socket.close();
        AppLog.add("PmrSocket: stop() — сокет закрыт");
    }

    public void sendVoice(byte[] mainPacket, byte[] reservePacket) {
        if (socket == null || socket.isClosed()) return;
        try {
            if (mainPacket != null) {
                socket.send(new DatagramPacket(mainPacket, mainPacket.length, serverAddr, portPrd + kanalPrd));
            }
        } catch (IOException e) {
            AppLog.add("PmrSocket: ОШИБКА sendVoice — " + e.getMessage());
        }
    }
}