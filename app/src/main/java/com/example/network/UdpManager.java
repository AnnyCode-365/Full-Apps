package com.example.network;

import com.example.ui.StatusManager;
import com.example.utils.NetworkUtils;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Gerenciador de tráfego UDP em tempo real.
 * Não bloqueia pacotes essenciais do sistema Android.
 * Compatível com Java 7.
 */
public class UdpManager {

    public static class UdpFlow {
        public final int sourcePort;
        public final int destPort;
        public long packetCount;
        public long byteCount;
        public long lastSeen;

        public UdpFlow(int sourcePort, int destPort) {
            this.sourcePort = sourcePort;
            this.destPort = destPort;
            this.lastSeen = System.currentTimeMillis();
        }
    }

    private final AtomicBoolean enabled = new AtomicBoolean(true);
    private final AtomicLong totalPackets = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    private final Map<String, UdpFlow> activeFlows = Collections.synchronizedMap(new HashMap<String, UdpFlow>());
    private String forwardingState = "Ativo (Filtro e Inspeção)";

    public UdpManager() {
    }

    public void setEnabled(boolean enabled) {
        this.enabled.set(enabled);
        StatusManager.getInstance().setUdpRunning(enabled);
        if (enabled) {
            forwardingState = "Ativo (Filtro e Inspeção)";
        } else {
            forwardingState = "Pass-Through (Monitoramento Pausado)";
        }
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    /**
     * Processa um pacote IP se for protocolo UDP (17).
     * Retorna se o pacote foi identificado como UDP.
     */
    public boolean processPacket(byte[] packet, int length, boolean isOutbound) {
        if (length < 20) return false;

        int protocol = NetworkUtils.parseProtocol(packet, length);
        if (protocol != NetworkUtils.PROTOCOL_UDP) {
            return false;
        }

        if (!enabled.get()) {
            return true; // É UDP mas monitoramento está pausado
        }

        int ipHdrLen = NetworkUtils.parseIpHeaderLength(packet, length);
        if (ipHdrLen < 20 || length < ipHdrLen + 8) {
            totalErrors.incrementAndGet();
            return true;
        }

        int srcPort = NetworkUtils.parseSourcePort(packet, length, ipHdrLen);
        int dstPort = NetworkUtils.parseDestPort(packet, length, ipHdrLen);

        int udpLength = ((packet[ipHdrLen + 4] & 0xFF) << 8) | (packet[ipHdrLen + 5] & 0xFF);
        if (udpLength < 8) {
            totalErrors.incrementAndGet();
            return true;
        }

        totalPackets.incrementAndGet();
        totalBytes.addAndGet(length);

        String flowKey = srcPort + "->" + dstPort;
        synchronized (activeFlows) {
            UdpFlow flow = activeFlows.get(flowKey);
            if (flow == null) {
                flow = new UdpFlow(srcPort, dstPort);
                activeFlows.put(flowKey, flow);
            }
            flow.packetCount++;
            flow.byteCount += length;
            flow.lastSeen = System.currentTimeMillis();

            // Limpa fluxos inativos há mais de 60 segundos
            if (activeFlows.size() > 50) {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<String, UdpFlow>> it = activeFlows.entrySet().iterator();
                while (it.hasNext()) {
                    if (now - it.next().getValue().lastSeen > 60000) {
                        it.remove();
                    }
                }
            }
        }

        StatusManager.getInstance().updateUdpMetrics(
                totalPackets.get(),
                totalBytes.get(),
                activeFlows.size()
        );

        return true;
    }

    public long getTotalPackets() { return totalPackets.get(); }
    public long getTotalBytes() { return totalBytes.get(); }
    public long getTotalErrors() { return totalErrors.get(); }
    public int getActiveFlowCount() { return activeFlows.size(); }
    public String getForwardingState() { return forwardingState; }

    public void reset() {
        totalPackets.set(0);
        totalBytes.set(0);
        totalErrors.set(0);
        synchronized (activeFlows) {
            activeFlows.clear();
        }
        StatusManager.getInstance().updateUdpMetrics(0, 0, 0);
    }
}
