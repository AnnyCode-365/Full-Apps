package com.example.vpn;

import android.net.VpnService;
import android.os.ParcelFileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Gerenciador da interface virtual TUN (ParcelFileDescriptor).
 * Criação, configuração de MTU, rotas e encerramento de descritores de arquivo.
 * Compatível com Java 7.
 */
public class TunManager {

    private final VpnService vpnService;
    private ParcelFileDescriptor vpnInterface;
    private FileInputStream inStream;
    private FileOutputStream outStream;

    private int mtu = 1500;
    private final String localAddress = "10.8.0.2";
    private final int prefixLength = 24;

    public TunManager(VpnService vpnService) {
        this.vpnService = vpnService;
    }

    public synchronized boolean establish(int configuredMtu, DnsManager dnsManager) {
        close();
        try {
            this.mtu = (configuredMtu >= 1280 && configuredMtu <= 1500) ? configuredMtu : 1500;

            VpnService.Builder builder = vpnService.new Builder();
            builder.setSession("GHOST NETWORK TUN")
                    .setMtu(this.mtu)
                    .addAddress(localAddress, prefixLength)
                    .addRoute("0.0.0.0", 0);

            if (dnsManager != null) {
                List<String> dnsList = dnsManager.getDnsServers();
                for (String dns : dnsList) {
                    try {
                        builder.addDnsServer(dns);
                    } catch (Exception ignored) {}
                }
            }

            builder.setBlocking(true);

            vpnInterface = builder.establish();
            if (vpnInterface != null) {
                inStream = new FileInputStream(vpnInterface.getFileDescriptor());
                outStream = new FileOutputStream(vpnInterface.getFileDescriptor());
                return true;
            }
        } catch (Exception e) {
            close();
        }
        return false;
    }

    public synchronized void close() {
        if (inStream != null) {
            try { inStream.close(); } catch (IOException ignored) {}
            inStream = null;
        }
        if (outStream != null) {
            try { outStream.close(); } catch (IOException ignored) {}
            outStream = null;
        }
        if (vpnInterface != null) {
            try { vpnInterface.close(); } catch (IOException ignored) {}
            vpnInterface = null;
        }
    }

    public boolean isEstablished() {
        return vpnInterface != null && vpnInterface.getFileDescriptor().valid();
    }

    public FileInputStream getInputStream() {
        return inStream;
    }

    public FileOutputStream getOutputStream() {
        return outStream;
    }

    public int getMtu() {
        return mtu;
    }
}
