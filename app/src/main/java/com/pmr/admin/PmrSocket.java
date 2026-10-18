package com.pmr.admin;

import android.util.Log;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

public class PmrSocket {
    private static final String TAG = "PmrSocket";
    private DatagramSocket socket;
    private InetAddress serverAddr;
    private int serverPort;
    private boolean isRunning = false;
    private Thread receiveThread;
    private AudioEngine audioEngine;

    public PmrSocket(String ip, int port) {
        try {
            this.serverAddr = InetAddress.getByName(ip);
            this.serverPort = port;
            this.socket = new DatagramSocket();
        } catch (Exception e) {
            Log.e(TAG, "Error init socket: " + e.getMessage());
        }
    }

    public void setAudioEngine(AudioEngine audioEngine) {
        this.audioEngine = audioEngine;
    }

    public void start() {
        if (isRunning) return;
        isRunning = true;
        receiveThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[2048];
                while (isRunning) {
                    try {
                        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                        socket.receive(packet);
                        byte[] data = new byte[packet.getLength()];
                        System.arraycopy(packet.getData(), 0, data, 0, packet.getLength());
                        
                        if (data.length > 4 && data[0] == 22) {
                            if (audioEngine != null) {
                                audioEngine.pushToRingBuffer(data);
                            }
                        }
                        
                    } catch (Exception e) {
                        // Игнорируем таймауты
                    }
                }
            }
        }, "PMR-ReceiveThread");
        receiveThread.start();
    }

    public void stop() {
        isRunning = false;
        if (socket != null && !socket.isClosed()) {
            socket.close();
        }
    }

    public void sendVoiceRaw(byte[] encodedG711) {
        if (socket == null || socket.isClosed()) return;
        try {
            byte[] packet = new byte[324];
            packet[0] = 22; // cmd=22
            int dataLen = Math.min(encodedG711.length, 320);
            System.arraycopy(encodedG711, 0, packet, 4, dataLen);
            
            DatagramPacket udpPacket = new DatagramPacket(packet, packet.length, serverAddr, serverPort);
            socket.send(udpPacket);
        } catch (IOException e) {
            Log.e(TAG, "Send voice error: " + e.getMessage());
        }
    }
}