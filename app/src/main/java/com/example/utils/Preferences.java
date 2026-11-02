package com.example.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Persistência de configurações e estados do GHOST NETWORK via SharedPreferences.
 * Compatível com Java 7 e ambientes AIDE.
 */
public class Preferences {

    private static final String PREF_NAME = "ghost_network_prefs";

    public static final String KEY_FLOATING_ENABLED = "floating_enabled";
    public static final String KEY_VPN_ENABLED = "vpn_enabled";
    public static final String KEY_JITTER_ENABLED = "jitter_enabled";
    public static final String KEY_UDP_ENABLED = "udp_enabled";
    public static final String KEY_DOWNLOAD_ENABLED = "download_enabled";
    public static final String KEY_UPLOAD_ENABLED = "upload_enabled";

    public static final String KEY_WINDOW_X = "window_x";
    public static final String KEY_WINDOW_Y = "window_y";
    public static final String KEY_WINDOW_ALPHA = "window_alpha";
    public static final String KEY_WINDOW_MINIMIZED = "window_minimized";

    public static final String KEY_JITTER_INTERVAL_MS = "jitter_interval_ms";
    public static final String KEY_JITTER_TARGET = "jitter_target";

    public static final String KEY_VPN_SERVER_HOST = "vpn_server_host";
    public static final String KEY_VPN_SERVER_PORT = "vpn_server_port";
    public static final String KEY_VPN_SECRET_KEY = "vpn_secret_key";
    public static final String KEY_VPN_MTU = "vpn_mtu";
    public static final String KEY_VPN_DNS = "vpn_dns";

    public static final String KEY_UNIT_MB = "unit_mb";

    private final SharedPreferences prefs;

    public Preferences(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isFloatingEnabled() {
        return prefs.getBoolean(KEY_FLOATING_ENABLED, false);
    }

    public void setFloatingEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_FLOATING_ENABLED, enabled).apply();
    }

    public boolean isVpnEnabled() {
        return prefs.getBoolean(KEY_VPN_ENABLED, false);
    }

    public void setVpnEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_VPN_ENABLED, enabled).apply();
    }

    public boolean isJitterEnabled() {
        return prefs.getBoolean(KEY_JITTER_ENABLED, true);
    }

    public void setJitterEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_JITTER_ENABLED, enabled).apply();
    }

    public boolean isUdpEnabled() {
        return prefs.getBoolean(KEY_UDP_ENABLED, true);
    }

    public void setUdpEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_UDP_ENABLED, enabled).apply();
    }

    public boolean isDownloadEnabled() {
        return prefs.getBoolean(KEY_DOWNLOAD_ENABLED, true);
    }

    public void setDownloadEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_DOWNLOAD_ENABLED, enabled).apply();
    }

    public boolean isUploadEnabled() {
        return prefs.getBoolean(KEY_UPLOAD_ENABLED, true);
    }

    public void setUploadEnabled(boolean enabled) {
        prefs.edit().putBoolean(KEY_UPLOAD_ENABLED, enabled).apply();
    }

    public int getWindowX() {
        return prefs.getInt(KEY_WINDOW_X, 100);
    }

    public void setWindowX(int x) {
        prefs.edit().putInt(KEY_WINDOW_X, x).apply();
    }

    public int getWindowY() {
        return prefs.getInt(KEY_WINDOW_Y, 200);
    }

    public void setWindowY(int y) {
        prefs.edit().putInt(KEY_WINDOW_Y, y).apply();
    }

    public float getWindowAlpha() {
        return prefs.getFloat(KEY_WINDOW_ALPHA, 0.95f);
    }

    public void setWindowAlpha(float alpha) {
        prefs.edit().putFloat(KEY_WINDOW_ALPHA, alpha).apply();
    }

    public boolean isWindowMinimized() {
        return prefs.getBoolean(KEY_WINDOW_MINIMIZED, false);
    }

    public void setWindowMinimized(boolean minimized) {
        prefs.edit().putBoolean(KEY_WINDOW_MINIMIZED, minimized).apply();
    }

    public long getJitterIntervalMs() {
        return prefs.getLong(KEY_JITTER_INTERVAL_MS, 1500L);
    }

    public void setJitterIntervalMs(long interval) {
        prefs.edit().putLong(KEY_JITTER_INTERVAL_MS, interval).apply();
    }

    public String getJitterTarget() {
        return prefs.getString(KEY_JITTER_TARGET, "8.8.8.8");
    }

    public void setJitterTarget(String target) {
        prefs.edit().putString(KEY_JITTER_TARGET, target).apply();
    }

    public String getVpnServerHost() {
        return prefs.getString(KEY_VPN_SERVER_HOST, "");
    }

    public void setVpnServerHost(String host) {
        prefs.edit().putString(KEY_VPN_SERVER_HOST, host).apply();
    }

    public int getVpnServerPort() {
        return prefs.getInt(KEY_VPN_SERVER_PORT, 1194);
    }

    public void setVpnServerPort(int port) {
        prefs.edit().putInt(KEY_VPN_SERVER_PORT, port).apply();
    }

    public String getVpnSecretKey() {
        return prefs.getString(KEY_VPN_SECRET_KEY, "");
    }

    public void setVpnSecretKey(String key) {
        prefs.edit().putString(KEY_VPN_SECRET_KEY, key).apply();
    }

    public int getVpnMtu() {
        return prefs.getInt(KEY_VPN_MTU, 1500);
    }

    public void setVpnMtu(int mtu) {
        prefs.edit().putInt(KEY_VPN_MTU, mtu).apply();
    }

    public String getVpnDns() {
        return prefs.getString(KEY_VPN_DNS, "8.8.8.8");
    }

    public void setVpnDns(String dns) {
        prefs.edit().putString(KEY_VPN_DNS, dns).apply();
    }

    public boolean isUnitMb() {
        return prefs.getBoolean(KEY_UNIT_MB, false);
    }

    public void setUnitMb(boolean unitMb) {
        prefs.edit().putBoolean(KEY_UNIT_MB, unitMb).apply();
    }
}
