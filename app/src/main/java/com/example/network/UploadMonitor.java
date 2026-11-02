package com.example.network;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Monitor independente de tráfego de Upload.
 * Compatível com Java 7.
 */
public class UploadMonitor {

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicLong currentSpeedBps = new AtomicLong(0);
    private final AtomicLong totalBytesSent = new AtomicLong(0);

    private long sessionStartTime = System.currentTimeMillis();
    private final List<Long> history = new ArrayList<Long>();
    private static final int MAX_HISTORY = 60;

    public UploadMonitor() {
        resetSession();
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        if (!enabled) {
            currentSpeedBps.set(0);
        }
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void recordBytes(long bytes) {
        if (enabled.get() && bytes > 0) {
            totalBytesSent.addAndGet(bytes);
        }
    }

    public void updateSpeedSample(long bytesInInterval, long intervalMs) {
        if (!enabled.get()) {
            currentSpeedBps.set(0);
            return;
        }
        if (intervalMs <= 0) intervalMs = 1000;
        long speed = (bytesInInterval * 1000L) / intervalMs;
        currentSpeedBps.set(speed);

        synchronized (history) {
            if (history.size() >= MAX_HISTORY) {
                history.remove(0);
            }
            history.add(speed);
        }
    }

    public long getCurrentSpeedBps() {
        return currentSpeedBps.get();
    }

    public long getTotalBytesSent() {
        return totalBytesSent.get();
    }

    public long getAverageSpeedBps() {
        long elapsedSec = (System.currentTimeMillis() - sessionStartTime) / 1000;
        if (elapsedSec <= 0) elapsedSec = 1;
        return totalBytesSent.get() / elapsedSec;
    }

    public void resetSession() {
        sessionStartTime = System.currentTimeMillis();
        totalBytesSent.set(0);
        currentSpeedBps.set(0);
        synchronized (history) {
            history.clear();
        }
    }

    public List<Long> getHistory() {
        synchronized (history) {
            return new ArrayList<Long>(history);
        }
    }
}
