package com.example.vpn;

import com.example.network.TrafficMonitor;
import com.example.network.UdpManager;
import com.example.utils.NetworkUtils;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Roteador de pacotes da interface TUN da VPN.
 * Processa pacotes IPv4, desvia para inspeção UDP e encaminha para o túnel remoto.
 * Compatível com Java 7.
 */
public class PacketRouter {

    private final TunManager tunManager;
    private final TunnelManager tunnelManager;
    private final UdpManager udpManager;
    private final DnsManager dnsManager;
    private final TrafficMonitor trafficMonitor;

    private final AtomicBoolean running = new AtomicBoolean(false);
    private Thread routerThread;

    public PacketRouter(TunManager tunManager,
                        TunnelManager tunnelManager,
                        UdpManager udpManager,
                        DnsManager dnsManager,
                        TrafficMonitor trafficMonitor) {
        this.tunManager = tunManager;
        this.tunnelManager = tunnelManager;
        this.udpManager = udpManager;
        this.dnsManager = dnsManager;
        this.trafficMonitor = trafficMonitor;
    }

    public synchronized void start() {
        if (running.get()) return;
        running.set(true);

        routerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                routeLoop();
            }
        }, "GhostPacketRouter");
        routerThread.start();
    }

    public synchronized void stop() {
        running.set(false);
        if (routerThread != null) {
            routerThread.interrupt();
            routerThread = null;
        }
    }

    public boolean isRunning() {
        return running.get();
    }

    private void routeLoop() {
        FileInputStream inStream = tunManager.getInputStream();
        FileOutputStream outStream = tunManager.getOutputStream();
        if (inStream == null) return;

        byte[] packetBuffer = new byte[32768];

        while (running.get() && !Thread.currentThread().isInterrupted()) {
            try {
                int length = inStream.read(packetBuffer);
                if (length <= 0) {
                    continue;
                }

                // Análise do pacote IPv4
                int version = NetworkUtils.parseIpVersion(packetBuffer, length);
                if (version != 4) {
                    continue;
                }

                int protocol = NetworkUtils.parseProtocol(packetBuffer, length);
                int ipHdrLen = NetworkUtils.parseIpHeaderLength(packetBuffer, length);

                // Se UDP, envia para monitoramento detalhado do UdpManager
                if (protocol == NetworkUtils.PROTOCOL_UDP) {
                    udpManager.processPacket(packetBuffer, length, true);

                    int dstPort = NetworkUtils.parseDestPort(packetBuffer, length, ipHdrLen);
                    if (dstPort == 53) {
                        dnsManager.recordDnsQuery();
                    }
                }

                // Registra tráfego na VPN
                if (trafficMonitor != null) {
                    trafficMonitor.recordVpnTxBytes(length);
                }

                // Encaminhamento
                if (tunnelManager.hasRemoteServer()) {
                    tunnelManager.sendPacket(packetBuffer, length);
                } else {
                    // Modo de Inspeção Local: se for ICMP Echo Request (Ping), responde localmente
                    if (protocol == NetworkUtils.PROTOCOL_ICMP && outStream != null) {
                        handleLocalIcmp(packetBuffer, length, ipHdrLen, outStream);
                    }
                }

            } catch (IOException e) {
                if (!running.get()) break;
            } catch (Exception ignored) {
            }
        }
    }

    /**
     * Responde a ping (ICMP Echo Request) localmente para verificação de funcionamento do TUN.
     */
    private void handleLocalIcmp(byte[] packet, int length, int ipHdrLen, FileOutputStream outStream) {
        if (length < ipHdrLen + 8) return;
        int type = packet[ipHdrLen] & 0xFF;
        if (type == 8) { // Echo Request
            // Inverte IPs origem e destino
            for (int i = 0; i < 4; i++) {
                byte tmp = packet[12 + i];
                packet[12 + i] = packet[16 + i];
                packet[16 + i] = tmp;
            }

            // Transforma em Echo Reply (Type 0)
            packet[ipHdrLen] = 0;

            // Recalcula checksum ICMP
            packet[ipHdrLen + 2] = 0;
            packet[ipHdrLen + 3] = 0;
            int icmpLen = length - ipHdrLen;
            int icmpChecksum = NetworkUtils.computeChecksum(packet, ipHdrLen, icmpLen);
            packet[ipHdrLen + 2] = (byte) ((icmpChecksum >> 8) & 0xFF);
            packet[ipHdrLen + 3] = (byte) (icmpChecksum & 0xFF);

            // Recalcula checksum IP
            packet[10] = 0;
            packet[11] = 0;
            int ipChecksum = NetworkUtils.computeChecksum(packet, 0, ipHdrLen);
            packet[10] = (byte) ((ipChecksum >> 8) & 0xFF);
            packet[11] = (byte) (ipChecksum & 0xFF);

            try {
                outStream.write(packet, 0, length);
                if (trafficMonitor != null) {
                    trafficMonitor.recordVpnRxBytes(length);
                }
            } catch (IOException ignored) {}
        }
    }
}
