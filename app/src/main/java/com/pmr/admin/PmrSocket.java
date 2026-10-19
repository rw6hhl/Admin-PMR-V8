package com.pmr.admin;

import android.content.Context;
import android.content.SharedPreferences;
import java.io.File;
import java.io.FileOutputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

public class PmrSocket {
    public static String IP_SERVER  = "185.221.154.39";
    public static final String IP_SERVER2 = "109.172.7.155";
    public static final int PORT_RESERVE = 16300;
    public static final int PORT_CMD     = 15999;
    public static final int PORT_CHECK   = 16013;
    public static int MyMailIndex = 51953;
    public static int MyPChannel  = 5;
    public static int Priznak_pmr = 26000;

    private final ListFile listFile;
    private final ChanList chanList;
    private final ActiveLog activeLog;
    private final WebLog webLog;
    private final CmdQueue cmdQueue;
    private final File filesDir;
    private final Context appCtx;

    private volatile DatagramSocket sock;
    private volatile InetAddress serverAddr;
    private volatile InetAddress serverAddr2;
    private int port_prm = 5323;
    private int port_prd = 16000;
    private int kanal_PRD = 0;
    private int kanal_Secret = 0;

    private volatile int active_client_num = -1;
    private volatile int active_tic = 0;
    private int KtoActiv = -1;
    private int KtoTic = 0;

    private volatile boolean running = false;
    private Thread threadUdp;
    private Thread threadTimer;
    private int activeLogTick = 0;
    private volatile AudioEngine audioEngine;

    public PmrSocket(Context ctx, ListFile lf, ChanList cl, ActiveLog al, WebLog wl, CmdQueue cq, File filesDir) {
        this.appCtx = ctx;
        this.listFile = lf;
        this.chanList = cl;
        this.activeLog = al;
        this.webLog = wl;
        this.cmdQueue = cq;
        this.filesDir = filesDir;
    }

    public void setAudioEngine(AudioEngine ae) { this.audioEngine = ae; }
    public int getActiveClient() { return active_client_num; }
    public boolean isRunning() { return running && sock != null && serverAddr != null; }
    public int getPortPrm() { return port_prm; }
    public int getPortPrd() { return port_prd; }
    public int getKanalPRD() { return kanal_PRD; }
    public int getKanalSecret() { return kanal_Secret; }
    public int getKanalSecretInstance() { return kanal_Secret; }
    public static int getKanalSecretStatic() { return 0; }
    public void sendRawPublic(byte[] buf) { sendRaw(buf); }

    public void sendVoice(byte[] mainPacket, byte[] reservePacket) {
        if (mainPacket == null) return;
        DatagramSocket s = sock;
        InetAddress a1 = serverAddr;
        InetAddress a2 = serverAddr2;
        if (s == null || a1 == null) return;
        try {
            s.send(new DatagramPacket(mainPacket, mainPacket.length, a1, port_prd + kanal_PRD));
        } catch (Exception e) {
            AppLog.add("sendVoice main FAIL: " + e);
        }
        if (reservePacket != null) {
            try {
                s.send(new DatagramPacket(reservePacket, reservePacket.length, a1, PORT_RESERVE + kanal_PRD));
            } catch (Exception ignored) {}
            if (a2 != null) {
                try {
                    s.send(new DatagramPacket(reservePacket, reservePacket.length, a2, PORT_RESERVE + kanal_PRD));
                } catch (Exception ignored) {}
            }
        }
    }

    public void reloadFromPrefs(Context ctx) { /* ... */ }
    public void start() { /* ... */ }
    public void stop() { /* ... */ }
    public void sendBan(int client) { /* ... */ }
    public void sendL() { /* ... */ }
    public void send260(int v) { /* ... */ }
    public void sendRename(String text) { /* ... */ }
    public void sendDelete(int id) { /* ... */ }
    public void sendList() { /* ... */ }
    public void processCommand(String raw) { /* ... */ }
}