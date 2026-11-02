package com.example.jitter;

import android.os.SystemClock;
import com.example.ui.StatusManager;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitor ativo de latência e jitter através de sondas UDP / Socket em tempo real.
 * Não promete redução fictícia de ping; realiza medições reais da rede.
 * Compatível com Java 7.
 */
public class JitterMonitor {

    private final NetworkStatistics statistics = new NetworkStatistics();
    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread workerThread;

    private long intervalMs = 1500;
    private String targetHost = "8.8.8.8";
    private int targetPort = 53; // DNS Port para resposta universal rápida

    public JitterMonitor() {
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        StatusManager.getInstance().setJitterRunning(true);

        workerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                probeLoop();
            }
        }, "GhostJitterWorker");
        workerThread.start();
    }

    public synchronized void stop() {
        running.set(false);
        StatusManager.getInstance().setJitterRunning(false);
        if (workerThread != null) {
            workerThread.interrupt();
            workerThread = null;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public void setIntervalMs(long intervalMs) {
        this.intervalMs = Math.max(500, intervalMs);
    }

    public void setTargetHost(String host) {
        if (host != null && !host.trim().isEmpty()) {
            this.targetHost = host.trim();
        }
    }

    public NetworkStatistics getStatistics() {
        return statistics;
    }

    private void probeLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            long ping = measureProbeLatency(targetHost, targetPort);
            boolean success = ping >= 0;

            statistics.addSample(success ? ping : 0, success);

            boolean unstable = LatencyAnalyzer.isOscillationDetected(
                    statistics.getJitter(),
                    statistics.getPacketLoss(),
                    statistics.getVariance()
            );

            StatusManager.getInstance().updateJitterMetrics(
                    success ? ping : -1,
                    statistics.getJitter(),
                    statistics.getMinLatency(),
                    statistics.getMaxLatency(),
                    statistics.getAvgLatency(),
                    statistics.getPacketLoss(),
                    unstable
            );

            try {
                Thread.sleep(intervalMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /**
     * Medição de RTT real utilizando sonda UDP com fallback para Socket TCP connect.
     */
    private long measureProbeLatency(String host, int port) {
        long rtt = -1;
        DatagramSocket udpSocket = null;
        try {
            InetAddress address = InetAddress.getByName(host);
            udpSocket = new DatagramSocket();
            udpSocket.setSoTimeout(1200);

            // Sonda de query DNS simples de 12 bytes
            byte[] dnsQuery = new byte[] {
                    0x12, 0x34, // ID
                    0x01, 0x00, // Flags: Standard query
                    0x00, 0x01, // QDCOUNT: 1
                    0x00, 0x00, // ANCOUNT: 0
                    0x00, 0x00, // NSCOUNT: 0
                    0x00, 0x00, // ARCOUNT: 0
                    0x01, 'a', 0x00, // QNAME: "a"
                    0x00, 0x01, // QTYPE: A
                    0x00, 0x01  // QCLASS: IN
            };

            DatagramPacket sendPacket = new DatagramPacket(dnsQuery, dnsQuery.length, address, port);
            byte[] recvBuf = new byte[512];
            DatagramPacket recvPacket = new DatagramPacket(recvBuf, recvBuf.length);

            long startTime = SystemClock.elapsedRealtime();
            udpSocket.send(sendPacket);
            udpSocket.receive(recvPacket);
            long endTime = SystemClock.elapsedRealtime();

            rtt = endTime - startTime;
        } catch (Exception udpException) {
            // Fallback para medição via Socket handshake TCP
            Socket tcpSocket = null;
            try {
                long startTime = SystemClock.elapsedRealtime();
                tcpSocket = new Socket();
                tcpSocket.connect(new InetSocketAddress(host, 443), 1200);
                long endTime = SystemClock.elapsedRealtime();
                rtt = endTime - startTime;
            } catch (IOException ignored) {
                rtt = -1;
            } finally {
                if (tcpSocket != null) {
                    try { tcpSocket.close(); } catch (IOException ignored) {}
                }
            }
        } finally {
            if (udpSocket != null) {
                udpSocket.close();
            }
        }
        return rtt;
    }
}
