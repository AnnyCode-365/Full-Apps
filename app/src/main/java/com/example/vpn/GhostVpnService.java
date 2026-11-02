package com.example.vpn;

import android.app.Notification;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkRequest;
import android.net.VpnService;
import android.os.Build;
import android.os.IBinder;
import com.example.network.TrafficMonitor;
import com.example.network.UdpManager;
import com.example.ui.StatusManager;
import com.example.utils.NotificationHelper;
import com.example.utils.Preferences;

/**
 * Serviço Android VPN principal (VpnService) do GHOST NETWORK.
 * Cria a interface virtual TUN, gerencia conexões e monitora o estado da rede.
 * Compatível com Java 7.
 */
public class GhostVpnService extends VpnService {

    public static final String ACTION_START = "com.example.vpn.START";
    public static final String ACTION_STOP = "com.example.vpn.STOP";

    private static GhostVpnService activeInstance;

    private TunManager tunManager;
    private TunnelManager tunnelManager;
    private PacketRouter packetRouter;
    private DnsManager dnsManager;
    private UdpManager udpManager;
    private TrafficMonitor trafficMonitor;
    private Preferences preferences;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    public static GhostVpnService getActiveInstance() {
        return activeInstance;
    }

    public static boolean isServiceRunning() {
        return activeInstance != null && activeInstance.tunManager != null && activeInstance.tunManager.isEstablished();
    }

    @Override
    public void onCreate() {
        super.onCreate();
        activeInstance = this;
        preferences = new Preferences(this);

        dnsManager = new DnsManager();
        tunManager = new TunManager(this);
        tunnelManager = new TunnelManager(this);
        udpManager = new UdpManager();
        trafficMonitor = new TrafficMonitor();

        packetRouter = new PacketRouter(
                tunManager,
                tunnelManager,
                udpManager,
                dnsManager,
                trafficMonitor
        );

        setupNetworkCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopVpn();
            return START_NOT_STICKY;
        }

        startVpn();
        return START_STICKY;
    }

    private void startVpn() {
        Notification notification = NotificationHelper.buildVpnNotification(this, "VPN Local Conectada e Operando");
        startForeground(NotificationHelper.NOTIF_ID_VPN, notification);

        // Configuração de MTU e DNS dos Preferences
        int mtu = preferences.getVpnMtu();
        dnsManager.setPrimaryDns(preferences.getVpnDns());

        boolean established = tunManager.establish(mtu, dnsManager);
        if (!established) {
            StatusManager.getInstance().updateVpnState("Falha ao criar interface TUN", false);
            stopSelf();
            return;
        }

        // Configuração do túnel remoto (se houver credenciais salvas)
        tunnelManager.configure(
                preferences.getVpnServerHost(),
                preferences.getVpnServerPort(),
                preferences.getVpnSecretKey()
        );

        if (tunnelManager.hasRemoteServer()) {
            tunnelManager.connect(tunManager.getOutputStream());
        } else {
            StatusManager.getInstance().updateVpnState("Inspeção Local Ativa (Sem servidor remoto)", false);
        }

        udpManager.setEnabled(preferences.isUdpEnabled());
        packetRouter.start();
        trafficMonitor.start();

        StatusManager.getInstance().setVpnRunning(true);
        preferences.setVpnEnabled(true);
    }

    private void stopVpn() {
        if (packetRouter != null) {
            packetRouter.stop();
        }
        if (tunnelManager != null) {
            tunnelManager.disconnect();
        }
        if (tunManager != null) {
            tunManager.close();
        }
        if (trafficMonitor != null) {
            trafficMonitor.stop();
        }

        StatusManager.getInstance().setVpnRunning(false);
        if (preferences != null) {
            preferences.setVpnEnabled(false);
        }

        stopForeground(true);
        stopSelf();
    }

    private void setupNetworkCallback() {
        try {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(Network network) {
                        // Reconexão de rede Wi-Fi / Dados Móveis
                        if (tunnelManager != null && tunnelManager.hasRemoteServer() && tunManager != null && tunManager.isEstablished()) {
                            tunnelManager.connect(tunManager.getOutputStream());
                        }
                    }

                    @Override
                    public void onLost(Network network) {
                        StatusManager.getInstance().updateVpnState("Conectividade de rede perdida", false);
                    }
                };
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        } catch (Exception ignored) {}
    }

    @Override
    public void onRevoke() {
        stopVpn();
        super.onRevoke();
    }

    @Override
    public void onDestroy() {
        activeInstance = null;
        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {}
        }
        stopVpn();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return super.onBind(intent);
    }

    public UdpManager getUdpManager() { return udpManager; }
    public TrafficMonitor getTrafficMonitor() { return trafficMonitor; }
    public TunnelManager getTunnelManager() { return tunnelManager; }
}
