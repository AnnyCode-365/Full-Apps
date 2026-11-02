package com.example.jitter;

import java.util.ArrayList;
import java.util.List;

/**
 * Modelo de dados estatísticos de latência, jitter e perda de pacotes.
 * Compatível com Java 7.
 */
public class NetworkStatistics {

    public static class Sample {
        public final long timestamp;
        public final long latencyMs;
        public final boolean success;

        public Sample(long timestamp, long latencyMs, boolean success) {
            this.timestamp = timestamp;
            this.latencyMs = latencyMs;
            this.success = success;
        }
    }

    private final List<Sample> samples = new ArrayList<Sample>();
    private static final int MAX_SAMPLES = 50;

    private long minLatency = Long.MAX_VALUE;
    private long maxLatency = 0;
    private long avgLatency = 0;
    private double jitter = 0.0;
    private double packetLoss = 0.0;
    private double variance = 0.0;

    private long baselineLatencyBeforeVpn = -1;
    private long currentLatencyWithVpn = -1;

    public synchronized void addSample(long latencyMs, boolean success) {
        Sample sample = new Sample(System.currentTimeMillis(), latencyMs, success);
        if (samples.size() >= MAX_SAMPLES) {
            samples.remove(0);
        }
        samples.add(sample);
        recalculate();
    }

    private void recalculate() {
        if (samples.isEmpty()) {
            minLatency = 0;
            maxLatency = 0;
            avgLatency = 0;
            jitter = 0;
            packetLoss = 0;
            variance = 0;
            return;
        }

        long sum = 0;
        int successCount = 0;
        int lostCount = 0;
        long min = Long.MAX_VALUE;
        long max = 0;

        List<Long> latencies = new ArrayList<Long>();

        for (Sample s : samples) {
            if (s.success) {
                successCount++;
                sum += s.latencyMs;
                latencies.add(s.latencyMs);
                if (s.latencyMs < min) min = s.latencyMs;
                if (s.latencyMs > max) max = s.latencyMs;
            } else {
                lostCount++;
            }
        }

        packetLoss = (double) lostCount / samples.size() * 100.0;

        if (successCount > 0) {
            minLatency = min;
            maxLatency = max;
            avgLatency = sum / successCount;

            // RFC 3550 / IPDV Jitter: average difference between consecutive latencies
            double jitterSum = 0;
            int diffCount = 0;
            for (int i = 1; i < latencies.size(); i++) {
                jitterSum += Math.abs(latencies.get(i) - latencies.get(i - 1));
                diffCount++;
            }
            jitter = (diffCount > 0) ? (jitterSum / diffCount) : 0;

            // Variance calculation
            double varSum = 0;
            for (Long lat : latencies) {
                double diff = lat - avgLatency;
                varSum += diff * diff;
            }
            variance = varSum / successCount;
        } else {
            minLatency = 0;
            maxLatency = 0;
            avgLatency = 0;
            jitter = 0;
            variance = 0;
        }
    }

    public synchronized void recordBaselineBeforeVpn(long latency) {
        this.baselineLatencyBeforeVpn = latency;
    }

    public synchronized void recordWithVpn(long latency) {
        this.currentLatencyWithVpn = latency;
    }

    public long getMinLatency() { return minLatency == Long.MAX_VALUE ? 0 : minLatency; }
    public long getMaxLatency() { return maxLatency; }
    public long getAvgLatency() { return avgLatency; }
    public double getJitter() { return jitter; }
    public double getPacketLoss() { return packetLoss; }
    public double getVariance() { return variance; }
    public long getBaselineLatencyBeforeVpn() { return baselineLatencyBeforeVpn; }
    public long getCurrentLatencyWithVpn() { return currentLatencyWithVpn; }

    public synchronized List<Sample> getSamples() {
        return new ArrayList<Sample>(samples);
    }
}
