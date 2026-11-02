package com.example.controllers;

import android.content.Context;
import com.example.network.UdpManager;
import com.example.ui.StatusManager;
import com.example.utils.Preferences;
import com.example.vpn.GhostVpnService;

/**
 * Controlador de gerenciamento de tráfego UDP.
 * Permite ligar/desligar o monitoramento de UDP sem interromper conexões ativas.
 * Compatível com Java 7.
 */
public class UdpController {

    private final Context context;
    private final Preferences preferences;

    public UdpController(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = new Preferences(context);
    }

    public void start() {
        preferences.setUdpEnabled(true);
        StatusManager.getInstance().setUdpRunning(true);
        GhostVpnService vpn = GhostVpnService.getActiveInstance();
        if (vpn != null) {
            UdpManager manager = vpn.getUdpManager();
            if (manager != null) {
                manager.setEnabled(true);
            }
        }
    }

    public void stop() {
        preferences.setUdpEnabled(false);
        StatusManager.getInstance().setUdpRunning(false);
        GhostVpnService vpn = GhostVpnService.getActiveInstance();
        if (vpn != null) {
            UdpManager manager = vpn.getUdpManager();
            if (manager != null) {
                manager.setEnabled(false);
            }
        }
    }

    public boolean isRunning() {
        return StatusManager.getInstance().isUdpRunning();
    }

    public String getStatus() {
        if (!isRunning()) {
            return "Monitor UDP Desativado (Pass-Through)";
        }
        return "UDP Ativo (" + StatusManager.getInstance().getUdpPacketCount() + " pacotes)";
    }
}
