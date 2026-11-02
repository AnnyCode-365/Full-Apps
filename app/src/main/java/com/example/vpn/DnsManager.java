package com.example.vpn;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
 * Gerenciador de servidores DNS e validação para a interface VPN TUN.
 * Compatível com Java 7.
 */
public class DnsManager {

    private String primaryDns = "8.8.8.8";
    private String secondaryDns = "1.1.1.1";
    private long dnsQueryCount = 0;

    public DnsManager() {
    }

    public void setPrimaryDns(String dns) {
        if (isValidIp(dns)) {
            this.primaryDns = dns;
        }
    }

    public void setSecondaryDns(String dns) {
        if (isValidIp(dns)) {
            this.secondaryDns = dns;
        }
    }

    public String getPrimaryDns() {
        return primaryDns;
    }

    public String getSecondaryDns() {
        return secondaryDns;
    }

    public List<String> getDnsServers() {
        List<String> list = new ArrayList<String>();
        list.add(primaryDns);
        if (secondaryDns != null && !secondaryDns.equals(primaryDns)) {
            list.add(secondaryDns);
        }
        return list;
    }

    public synchronized void recordDnsQuery() {
        dnsQueryCount++;
    }

    public synchronized long getDnsQueryCount() {
        return dnsQueryCount;
    }

    public static boolean isValidIp(String ip) {
        if (ip == null || ip.trim().isEmpty()) return false;
        try {
            String[] parts = ip.trim().split("\\.");
            if (parts.length != 4) return false;
            for (String p : parts) {
                int val = Integer.parseInt(p);
                if (val < 0 || val > 255) return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
