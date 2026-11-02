package com.example.network;

import android.net.TrafficStats;
import com.example.ui.StatusManager;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Coordenador de monitoramento de tráfego de download e upload em tempo real.
 * Utiliza deltas do TrafficStats do Android combinados com métricas da interface TUN.
 * Compatível com Java 7.
 */
public class TrafficMonitor {

    private final DownloadMonitor downloadMonitor = new DownloadMonitor();
    private final UploadMonitor uploadMonitor = new UploadMonitor();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread monitorThread;

    private long lastRxBytes = 0;
    private long lastTxBytes = 0;
    private long lastSampleTime = 0;

    public TrafficMonitor() {
        lastRxBytes = TrafficStats.getTotalRxBytes();
        lastTxBytes = TrafficStats.getTotalTxBytes();
        lastSampleTime = System.currentTimeMillis();
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);

        lastRxBytes = TrafficStats.getTotalRxBytes();
        lastTxBytes = TrafficStats.getTotalTxBytes();
        lastSampleTime = System.currentTimeMillis();

        monitorThread = new Thread(new Runnable() {
            @Override
            public void run() {
                samplingLoop();
            }
        }, "GhostTrafficSampler");
        monitorThread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (monitorThread != null) {
            monitorThread.interrupt();
            monitorThread = null;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    public DownloadMonitor getDownloadMonitor() {
        return downloadMonitor;
    }

    public UploadMonitor getUploadMonitor() {
        return uploadMonitor;
    }

    /**
     * Registra bytes recebidos diretamente da interface TUN da VPN.
     */
    public void recordVpnRxBytes(long bytes) {
        downloadMonitor.recordBytes(bytes);
    }

    /**
     * Registra bytes transmitidos diretamente para a interface TUN da VPN.
     */
    public void recordVpnTxBytes(long bytes) {
        uploadMonitor.recordBytes(bytes);
    }

    private void samplingLoop() {
        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                break;
            }

            long currentRx = TrafficStats.getTotalRxBytes();
            long currentTx = TrafficStats.getTotalTxBytes();
            long currentTime = System.currentTimeMillis();

            long intervalMs = currentTime - lastSampleTime;
            if (intervalMs <= 0) intervalMs = 1000;

            long deltaRx = (currentRx != TrafficStats.UNSUPPORTED && currentRx >= lastRxBytes)
                    ? (currentRx - lastRxBytes) : 0;
            long deltaTx = (currentTx != TrafficStats.UNSUPPORTED && currentTx >= lastTxBytes)
                    ? (currentTx - lastTxBytes) : 0;

            if (currentRx != TrafficStats.UNSUPPORTED) lastRxBytes = currentRx;
            if (currentTx != TrafficStats.UNSUPPORTED) lastTxBytes = currentTx;
            lastSampleTime = currentTime;

            downloadMonitor.recordBytes(deltaRx);
            uploadMonitor.recordBytes(deltaTx);

            downloadMonitor.updateSpeedSample(deltaRx, intervalMs);
            uploadMonitor.updateSpeedSample(deltaTx, intervalMs);

            StatusManager.getInstance().updateTrafficMetrics(
                    downloadMonitor.getCurrentSpeedBps(),
                    uploadMonitor.getCurrentSpeedBps(),
                    downloadMonitor.getTotalBytesReceived(),
                    uploadMonitor.getTotalBytesSent()
            );
        }
    }
}
