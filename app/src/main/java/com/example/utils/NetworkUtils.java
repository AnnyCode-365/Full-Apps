package com.example.utils;

import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Utilitários de rede, formatação e análise de pacotes IP / UDP / TCP.
 * Compatível com Java 7.
 */
public class NetworkUtils {

    public static final int PROTOCOL_ICMP = 1;
    public static final int PROTOCOL_TCP = 6;
    public static final int PROTOCOL_UDP = 17;

    public static String formatSpeed(long bytesPerSec, boolean preferMb) {
        if (bytesPerSec < 0) {
            bytesPerSec = 0;
        }
        if (preferMb) {
            double mb = bytesPerSec / (1024.0 * 1024.0);
            return String.format(Locale.US, "%.2f MB/s", mb);
        } else {
            if (bytesPerSec >= 1024 * 1024) {
                double mb = bytesPerSec / (1024.0 * 1024.0);
                return String.format(Locale.US, "%.2f MB/s", mb);
            } else if (bytesPerSec >= 1024) {
                double kb = bytesPerSec / 1024.0;
                return String.format(Locale.US, "%.1f KB/s", kb);
            } else {
                return bytesPerSec + " B/s";
            }
        }
    }

    public static String formatBytes(long bytes) {
        if (bytes < 0) {
            bytes = 0;
        }
        if (bytes >= 1024L * 1024L * 1024L) {
            double gb = bytes / (1024.0 * 1024.0 * 1024.0);
            return String.format(Locale.US, "%.2f GB", gb);
        } else if (bytes >= 1024L * 1024L) {
            double mb = bytes / (1024.0 * 1024.0);
            return String.format(Locale.US, "%.1f MB", mb);
        } else if (bytes >= 1024L) {
            double kb = bytes / 1024.0;
            return String.format(Locale.US, "%.1f KB", kb);
        } else {
            return bytes + " B";
        }
    }

    public static String formatLatency(long latencyMs) {
        if (latencyMs < 0) {
            return "-- ms";
        }
        return latencyMs + " ms";
    }

    public static String formatJitter(double jitterMs) {
        if (jitterMs < 0) {
            return "-- ms";
        }
        return String.format(Locale.US, "%.1f ms", jitterMs);
    }

    public static String intToIp(int ip) {
        return ((ip >> 24) & 0xFF) + "." +
               ((ip >> 16) & 0xFF) + "." +
               ((ip >> 8) & 0xFF) + "." +
               (ip & 0xFF);
    }

    public static int parseIpVersion(byte[] packet, int length) {
        if (length < 20) return -1;
        return (packet[0] >> 4) & 0x0F;
    }

    public static int parseIpHeaderLength(byte[] packet, int length) {
        if (length < 20) return -1;
        return (packet[0] & 0x0F) * 4;
    }

    public static int parseProtocol(byte[] packet, int length) {
        if (length < 20) return -1;
        return packet[9] & 0xFF;
    }

    public static String parseSourceIp(byte[] packet, int length) {
        if (length < 20) return "";
        try {
            byte[] src = new byte[4];
            System.arraycopy(packet, 12, src, 0, 4);
            return InetAddress.getByAddress(src).getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

    public static String parseDestIp(byte[] packet, int length) {
        if (length < 20) return "";
        try {
            byte[] dst = new byte[4];
            System.arraycopy(packet, 16, dst, 0, 4);
            return InetAddress.getByAddress(dst).getHostAddress();
        } catch (Exception e) {
            return "";
        }
    }

    public static int parseSourcePort(byte[] packet, int length, int ipHeaderLength) {
        if (length < ipHeaderLength + 4) return -1;
        int p1 = packet[ipHeaderLength] & 0xFF;
        int p2 = packet[ipHeaderLength + 1] & 0xFF;
        return (p1 << 8) | p2;
    }

    public static int parseDestPort(byte[] packet, int length, int ipHeaderLength) {
        if (length < ipHeaderLength + 4) return -1;
        int p1 = packet[ipHeaderLength + 2] & 0xFF;
        int p2 = packet[ipHeaderLength + 3] & 0xFF;
        return (p1 << 8) | p2;
    }

    public static int computeChecksum(byte[] buffer, int offset, int length) {
        int sum = 0;
        int idx = offset;
        while (length > 1) {
            sum += ((buffer[idx] & 0xFF) << 8) | (buffer[idx + 1] & 0xFF);
            idx += 2;
            length -= 2;
        }
        if (length > 0) {
            sum += (buffer[idx] & 0xFF) << 8;
        }
        while ((sum >> 16) > 0) {
            sum = (sum & 0xFFFF) + (sum >> 16);
        }
        return ~sum & 0xFFFF;
    }
}
