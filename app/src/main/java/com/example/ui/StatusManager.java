package com.example.ui;

import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Gerenciador central de status, métricas e eventos do Ghost Network.
 * Notifica a interface flutuante e a MainActivity em tempo real.
 * Compatível com Java 7.
 */
public class StatusManager {

    private static StatusManager instance;

    public interface StatusListener {
        void onStatusUpdated(StatusManager manager);
    }

    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final List<StatusListener> listeners = Collections.synchronizedList(new ArrayList<StatusListener>());

    // Estados dos serviços
    private boolean floatingRunning = false;
    private boolean vpnRunning = false;
    private boolean jitterRunning = true;
    private boolean udpRunning = true;
    private boolean downloadRunning = true;
    private boolean uploadRunning = true;

    // Métricas de Jitter / Latência
    private long pingMs = 0;
    private double jitterMs = 0.0;
    private long minPingMs = 0;
    private long maxPingMs = 0;
    private long avgPingMs = 0;
    private double packetLossPercent = 0.0;
    private boolean networkUnstable = false;

    // Métricas de Tráfego
    private long downloadSpeedBps = 0;
    private long uploadSpeedBps = 0;
    private long totalRxBytes = 0;
    private long totalTxBytes = 0;

    // Métricas de UDP
    private long udpPacketCount = 0;
    private long udpByteCount = 0;
    private int udpActiveFlows = 0;

    // Estado da VPN
    private String vpnStatusMessage = "Desconectado";
    private boolean tunnelSecured = false;

    // Histórico para gráficos em tempo real (últimos 30 pontos)
    private static final int MAX_HISTORY = 30;
    private final List<Float> downloadHistory = new ArrayList<Float>();
    private final List<Float> uploadHistory = new ArrayList<Float>();
    private final List<Float> latencyHistory = new ArrayList<Float>();

    public static synchronized StatusManager getInstance() {
        if (instance == null) {
            instance = new StatusManager();
        }
        return instance;
    }

    private StatusManager() {
        for (int i = 0; i < MAX_HISTORY; i++) {
            downloadHistory.add(0.0f);
            uploadHistory.add(0.0f);
            latencyHistory.add(0.0f);
        }
    }

    public void addListener(StatusListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            listener.onStatusUpdated(this);
        }
    }

    public void removeListener(StatusListener listener) {
        if (listener != null) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners() {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                synchronized (listeners) {
                    for (StatusListener l : listeners) {
                        try {
                            l.onStatusUpdated(StatusManager.this);
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        });
    }

    // Setters com notificação
    public synchronized void setFloatingRunning(boolean running) {
        this.floatingRunning = running;
        notifyListeners();
    }

    public synchronized void setVpnRunning(boolean running) {
        this.vpnRunning = running;
        if (!running) {
            this.vpnStatusMessage = "Desconectado";
            this.tunnelSecured = false;
        }
        notifyListeners();
    }

    public synchronized void setJitterRunning(boolean running) {
        this.jitterRunning = running;
        notifyListeners();
    }

    public synchronized void setUdpRunning(boolean running) {
        this.udpRunning = running;
        notifyListeners();
    }

    public synchronized void setDownloadRunning(boolean running) {
        this.downloadRunning = running;
        notifyListeners();
    }

    public synchronized void setUploadRunning(boolean running) {
        this.uploadRunning = running;
        notifyListeners();
    }

    public synchronized void updateJitterMetrics(long ping, double jitter, long min, long max, long avg, double loss, boolean unstable) {
        this.pingMs = ping;
        this.jitterMs = jitter;
        this.minPingMs = min;
        this.maxPingMs = max;
        this.avgPingMs = avg;
        this.packetLossPercent = loss;
        this.networkUnstable = unstable;

        if (ping > 0) {
            addHistoryPoint(latencyHistory, (float) ping);
        }
        notifyListeners();
    }

    public synchronized void updateTrafficMetrics(long dlSpeed, long ulSpeed, long rxBytes, long txBytes) {
        this.downloadSpeedBps = dlSpeed;
        this.uploadSpeedBps = ulSpeed;
        this.totalRxBytes = rxBytes;
        this.totalTxBytes = txBytes;

        addHistoryPoint(downloadHistory, (float) (dlSpeed / 1024.0)); // em KB/s
        addHistoryPoint(uploadHistory, (float) (ulSpeed / 1024.0)); // em KB/s
        notifyListeners();
    }

    public synchronized void updateUdpMetrics(long packets, long bytes, int flows) {
        this.udpPacketCount = packets;
        this.udpByteCount = bytes;
        this.udpActiveFlows = flows;
        notifyListeners();
    }

    public synchronized void updateVpnState(String message, boolean secured) {
        this.vpnStatusMessage = message;
        this.tunnelSecured = secured;
        notifyListeners();
    }

    private void addHistoryPoint(List<Float> list, float val) {
        if (list.size() >= MAX_HISTORY) {
            list.remove(0);
        }
        list.add(val);
    }

    // Getters
    public boolean isFloatingRunning() { return floatingRunning; }
    public boolean isVpnRunning() { return vpnRunning; }
    public boolean isJitterRunning() { return jitterRunning; }
    public boolean isUdpRunning() { return udpRunning; }
    public boolean isDownloadRunning() { return downloadRunning; }
    public boolean isUploadRunning() { return uploadRunning; }

    public long getPingMs() { return pingMs; }
    public double getJitterMs() { return jitterMs; }
    public long getMinPingMs() { return minPingMs; }
    public long getMaxPingMs() { return maxPingMs; }
    public long getAvgPingMs() { return avgPingMs; }
    public double getPacketLossPercent() { return packetLossPercent; }
    public boolean isNetworkUnstable() { return networkUnstable; }

    public long getDownloadSpeedBps() { return downloadSpeedBps; }
    public long getUploadSpeedBps() { return uploadSpeedBps; }
    public long getTotalRxBytes() { return totalRxBytes; }
    public long getTotalTxBytes() { return totalTxBytes; }

    public long getUdpPacketCount() { return udpPacketCount; }
    public long getUdpByteCount() { return udpByteCount; }
    public int getUdpActiveFlows() { return udpActiveFlows; }

    public String getVpnStatusMessage() { return vpnStatusMessage; }
    public boolean isTunnelSecured() { return tunnelSecured; }

    public synchronized List<Float> getDownloadHistory() {
        return new ArrayList<Float>(downloadHistory);
    }

    public synchronized List<Float> getUploadHistory() {
        return new ArrayList<Float>(uploadHistory);
    }

    public synchronized List<Float> getLatencyHistory() {
        return new ArrayList<Float>(latencyHistory);
    }
}
