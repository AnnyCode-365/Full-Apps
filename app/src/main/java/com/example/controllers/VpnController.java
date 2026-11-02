package com.example.controllers;

import android.content.Context;
import android.content.Intent;
import android.net.VpnService;
import android.os.Build;
import com.example.ui.StatusManager;
import com.example.utils.Preferences;
import com.example.vpn.GhostVpnService;

/**
 * Controlador de ciclo de vida da VPN Local.
 * Compatível com Java 7.
 */
public class VpnController {

    private final Context context;
    private final Preferences preferences;

    public VpnController(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = new Preferences(context);
    }

    public Intent prepareVpn() {
        return VpnService.prepare(context);
    }

    public void start() {
        Intent intent = new Intent(context, GhostVpnService.class);
        intent.setAction(GhostVpnService.ACTION_START);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent);
        } else {
            context.startService(intent);
        }
        preferences.setVpnEnabled(true);
    }

    public void stop() {
        Intent intent = new Intent(context, GhostVpnService.class);
        intent.setAction(GhostVpnService.ACTION_STOP);
        context.startService(intent);
        preferences.setVpnEnabled(false);
        StatusManager.getInstance().setVpnRunning(false);
    }

    public boolean isRunning() {
        return StatusManager.getInstance().isVpnRunning() || GhostVpnService.isServiceRunning();
    }

    public String getStatus() {
        if (!isRunning()) {
            return "VPN Desconectada";
        }
        return StatusManager.getInstance().getVpnStatusMessage();
    }
}
