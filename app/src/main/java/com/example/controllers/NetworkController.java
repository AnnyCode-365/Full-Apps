package com.example.controllers;

import android.content.Context;
import com.example.jitter.JitterMonitor;
import com.example.network.TrafficMonitor;
import com.example.ui.StatusManager;
import com.example.utils.Preferences;

/**
 * Controlador de telemetria de rede (Download, Upload e Jitter).
 * Compatível com Java 7.
 */
public class NetworkController {

    private static NetworkController instance;

    private final Context context;
    private final Preferences preferences;
    private final JitterMonitor jitterMonitor;
    private final TrafficMonitor trafficMonitor;

    public static synchronized NetworkController getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkController(context.getApplicationContext());
        }
        return instance;
    }

    private NetworkController(Context context) {
        this.context = context;
        this.preferences = new Preferences(context);
        this.jitterMonitor = new JitterMonitor();
        this.trafficMonitor = new TrafficMonitor();

        this.jitterMonitor.setIntervalMs(preferences.getJitterIntervalMs());
        this.jitterMonitor.setTargetHost(preferences.getJitterTarget());

        this.trafficMonitor.getDownloadMonitor().setEnabled(preferences.isDownloadEnabled());
        this.trafficMonitor.getUploadMonitor().setEnabled(preferences.isUploadEnabled());

        if (preferences.isJitterEnabled()) {
            jitterMonitor.start();
        }
        trafficMonitor.start();
    }

    public void setJitterEnabled(boolean enabled) {
        preferences.setJitterEnabled(enabled);
        StatusManager.getInstance().setJitterRunning(enabled);
        if (enabled) {
            jitterMonitor.start();
        } else {
            jitterMonitor.stop();
        }
    }

    public void setDownloadEnabled(boolean enabled) {
        preferences.setDownloadEnabled(enabled);
        StatusManager.getInstance().setDownloadRunning(enabled);
        trafficMonitor.getDownloadMonitor().setEnabled(enabled);
    }

    public void setUploadEnabled(boolean enabled) {
        preferences.setUploadEnabled(enabled);
        StatusManager.getInstance().setUploadRunning(enabled);
        trafficMonitor.getUploadMonitor().setEnabled(enabled);
    }

    public boolean isJitterRunning() {
        return jitterMonitor.isRunning();
    }

    public boolean isDownloadRunning() {
        return trafficMonitor.getDownloadMonitor().isEnabled();
    }

    public boolean isUploadRunning() {
        return trafficMonitor.getUploadMonitor().isEnabled();
    }

    public JitterMonitor getJitterMonitor() {
        return jitterMonitor;
    }

    public TrafficMonitor getTrafficMonitor() {
        return trafficMonitor;
    }

    public String getStatus() {
        return "Ping: " + StatusManager.getInstance().getPingMs() + "ms | Jitter: " +
                String.format("%.1f", StatusManager.getInstance().getJitterMs()) + "ms";
    }
}
